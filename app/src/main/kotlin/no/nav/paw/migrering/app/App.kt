package no.nav.paw.migrering.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener
import no.nav.paw.migrering.app.konfigurasjon.*
import no.nav.paw.migrering.app.ktor.initKtor
import no.nav.paw.migrering.app.serde.ArbeidssoekerEventSerde
import no.nav.paw.migrering.app.utils.consumerSequence
import org.apache.kafka.common.serialization.Serdes
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

val logger = LoggerFactory.getLogger("migrering")!!

fun main() {
    val dependencies = createDependencies(
        config = applikasjonKonfigurasjon,
        databaseKonfigurasjon = databaseKonfigurasjon
    )
    flywayMigrate(dependencies.dataSource)
    Database.connect(dependencies.dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val consumerStatus = StatusConsumerRebalanceListener(
        kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic,
        kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic
    )
    val ktorEngine = initKtor(
        prometheusMeterRegistry = prometheusMeterRegistry,
        statusConsumerRebalanceListener = consumerStatus
    )
    ktorEngine.start(wait = false)
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
    val avslutt = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Avslutter migrering")
        periodeSequence.closeJustLogOnError()
        besvarelseSequence.closeJustLogOnError()
        avslutt.set(true)
    })
    val value = System.getProperty("paw_migrering_deaktivert")
    if (value != null) {
        Thread.sleep(Duration.ofDays(1).toMillis())
    } else {
        use(
            periodeSequence,
            besvarelseSequence,
            hendelseProducer(kafkaKonfigurasjon)
        ) { periodeHendelseMeldinger, besvarelseHendelser, hendelseProducer ->
            with(prometheusMeterRegistry) {
                prepareBatches(
                    periodeHendelseMeldinger = periodeHendelseMeldinger,
                    besvarelseHendelser = besvarelseHendelser
                ).processBatches(
                    consumerStatus = consumerStatus,
                    eventlogTopic = kafkaKonfigurasjon.klientKonfigurasjon.eventlogTopic,
                    producer = hendelseProducer,
                    identitetsnummerTilKafkaKey = { identitetsnummer ->
                        runBlocking { dependencies.kafkaKeysClient.getKey(identitetsnummer).id }
                    })
            }
        }
    }
}
