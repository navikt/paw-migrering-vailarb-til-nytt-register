package no.nav.paw.migrering.app

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.db.skrivBatchTilDb
import no.nav.paw.migrering.app.kafka.*
import no.nav.paw.migrering.app.konfigurasjon.*
import no.nav.paw.migrering.app.ktor.initKtor
import no.nav.paw.migrering.app.serde.hendelseTilBytes
import no.nav.paw.migrering.app.utils.invoke
import no.nav.paw.migrering.app.utils.nLimitFilter
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

val logger = LoggerFactory.getLogger("migrering")!!

fun main() {
    val dependencies = createDependencies(
        config = applikasjonKonfigurasjon,
        databaseKonfigurasjon = databaseKonfigurasjon
    )
    flywayMigrate(dependencies.dataSource)
    Database.connect(dependencies.dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val avslutt = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Avslutter migrering")
        avslutt.set(true)
    })
    val consumerStatus = StatusConsumerRebalanceListener(
        kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic,
        kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic
    )
    val ktorEngine = initKtor(
        prometheusMeterRegistry = prometheusMeterRegistry,
        statusConsumerRebalanceListener = consumerStatus
    )
    ktorEngine.start(wait = false)
    use(
        periodeConsumer(kafkaKonfigurasjon),
        besvarelseConsumer(kafkaKonfigurasjon),
        hendelseProducer(kafkaKonfigurasjon)
    ) { periodeConsumer, besvarelseConsumer, hendelseProducer ->
        periodeConsumer.subscribe(consumerStatus, kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic)
        besvarelseConsumer.subscribe(consumerStatus, kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic)
        val utfoertAv = Bruker(
            type = BrukerType.SYSTEM,
            id = applikasjonKonfigurasjon.applicationName
        )
        val perioder: Sequence<Pair<Boolean, List<Hendelse>>> = periodeConsumer.asSequence(
            avslutt = avslutt,
            mapper = (::tilPeriode)(utfoertAv)
        ).map { periodeBatch -> consumerStatus.isReady(periodeConsumer.subscription()) to periodeBatch }
        val opplysninger: Sequence<Pair<Boolean, List<Hendelse>>> = besvarelseConsumer.asSequence(
            avslutt = avslutt,
            mapper = (::situasjonMottat)(utfoertAv)
        ).map { situasjonBatch -> consumerStatus.isReady(besvarelseConsumer.subscription()) to situasjonBatch }

        val tomBatchTeller = AtomicLong(0)
        perioder.zip(opplysninger)
            .map { (perioder, situasjoner) -> (perioder.first && situasjoner.first) to (perioder.second + situasjoner.second) }
            .nLimitFilter(numberOfConsecutiveFalseBeforeForward = 3) { (_, hendelser) -> hendelser.isNotEmpty() }
            .forEach { (erKlar, hendelser) ->
                with(prometheusMeterRegistry) {
                    when {
                        erKlar && hendelser.isEmpty() -> skrivTilTopic(
                            kafkaKonfigurasjon.klientKonfigurasjon.eventlogTopic,
                            hendelseProducer,
                            dependencies.kafkaKeysClient
                        )

                        hendelser.isNotEmpty() -> skrivBatchTilDb(serializer = hendelseTilBytes, batch = hendelser)

                        else -> {
                            if (tomBatchTeller.get() % 30 == 0L) {
                                logger.info("Venter på at alle topics skal være klare")
                            }
                        }
                    }
                }
            }
    }
}
