package no.nav.paw.migrering.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.app.db.HendelserTabell
import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysResponse
import no.nav.paw.migrering.app.konfigurasjon.*
import no.nav.paw.migrering.app.ktor.initKtor
import no.nav.paw.migrering.app.serde.ArbeidssoekerEventSerde
import no.nav.paw.migrering.app.serde.HendelseSerde
import no.nav.paw.migrering.app.utils.consumerSequence
import org.apache.kafka.common.serialization.Serdes
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

val logger = LoggerFactory.getLogger("migrering")!!
val antallHendelserIDB = AtomicLong(0)
fun main() {
    val dependencies = createDependencies(
        config = applikasjonKonfigurasjon,
        databaseKonfigurasjon = databaseKonfigurasjon
    )
    flywayMigrate(dependencies.dataSource)
    Database.connect(dependencies.dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val idfunksjon: (String) -> KafkaKeysResponse = { identitetsnummer ->
        runBlocking { dependencies.kafkaKeysClient.getKey(identitetsnummer) }
    }
    val consumerStatus = StatusConsumerRebalanceListener(
        kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic,
        kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic
    )
    val periodeConsumerProperties = kafkaKonfigurasjon.properties
        .medKeySerde(Serdes.String())
        .medValueSerde(ArbeidssoekerEventSerde())
    val besvarelseConsumerProperties = kafkaKonfigurasjon.propertiesMedAvroSchemaReg
        .medKeySerde(Serdes.String())
        .medValueSerde(SpecificAvroSerde<ArbeidssokerBesvarelseEvent>())
    val periodeSequence = consumerSequence<String, ArbeidssokerperiodeHendelseMelding>(
        consumerProperties = periodeConsumerProperties,
        subscribeTo = listOf(kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic),
        rebalanceListener = consumerStatus
    )
    val besvarelseSequence = consumerSequence<String, ArbeidssokerBesvarelseEvent>(
        consumerProperties = besvarelseConsumerProperties,
        subscribeTo = listOf(kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic),
        rebalanceListener = consumerStatus
    )
    val opplysnigngerFraVeilarbConsumerProperties = kafkaKonfigurasjon.properties
        .medKeySerde(Serdes.String())
        .medValueSerde(HendelseSerde())
    val opplysnigngerFraVeilarbSequence = consumerSequence<String, OpplysningerOmArbeidssoekerMottatt>(
        consumerProperties = opplysnigngerFraVeilarbConsumerProperties,
        subscribeTo = listOf(kafkaKonfigurasjon.klientKonfigurasjon.opplysningerFraVeilarbTopic),
        rebalanceListener = consumerStatus
    )
    val ktorEngine = initKtor(
        prometheusMeterRegistry = prometheusMeterRegistry,
        statusConsumerRebalanceListener = consumerStatus,
        kafkaClientMetrics = listOf(
            periodeSequence.metricsBinder,
            besvarelseSequence.metricsBinder,
            opplysnigngerFraVeilarbSequence.metricsBinder
        )
    )
    ktorEngine.start(wait = false)
    val avslutt = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Avslutter migrering")
        periodeSequence.closeJustLogOnError()
        besvarelseSequence.closeJustLogOnError()
        avslutt.set(true)
    })
    val value = System.getProperty("paw_migrering_deaktivert")
    if (value != null) {
        logger.info("Migrering er deaktivert")
        Thread.sleep(Duration.ofDays(1).toMillis())
    } else {
        logger.info("Migrering er aktivert")
        val antallHendelser = transaction {
            HendelserTabell.select { Op.TRUE }.count().toDouble()
        }
        antallHendelserIDB.set(antallHendelser.toLong())
        logger.info("Antall hendelser i DB ved start: $antallHendelser")
        prometheusMeterRegistry.gauge(MIGRERINGS_HENDELSER_I_DB, antallHendelserIDB) { it.get().toDouble() }
        use(
            periodeSequence,
            besvarelseSequence,
            opplysnigngerFraVeilarbSequence,
            hendelseProducer(kafkaKonfigurasjon)
        ) { periodeHendelseMeldinger, besvarelseHendelser, opplysningerFraVeilarb, hendelseProducer ->
            with(prometheusMeterRegistry) {
                prepareBatches(
                    periodeHendelseMeldinger = periodeHendelseMeldinger,
                    besvarelseHendelser = besvarelseHendelser,
                    opplysningerFraVeilarbHendelser = opplysningerFraVeilarb,
                    numberOfConsecutiveEmptyBatchesToWaitFor = 160,// each empty batch takes 375ms, 3 listeners with 125ms poll timeout
                    idfunksjon = idfunksjon
                ).processBatches(
                    consumerStatus = consumerStatus,
                    eventlogTopic = kafkaKonfigurasjon.klientKonfigurasjon.eventlogTopic,
                    producer = hendelseProducer,
                    identitetsnummerTilKafkaKey = idfunksjon
                )
            }
        }
    }
}
