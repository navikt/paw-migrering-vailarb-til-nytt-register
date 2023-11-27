package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.db.skrivBatchTilDb
import no.nav.paw.migrering.app.kafka.*
import no.nav.paw.migrering.app.konfigurasjon.*
import no.nav.paw.migrering.app.serde.hendelseTilBytes
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
    val avslutt = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Avslutter migrering")
        avslutt.set(true)
    })

    use(
        periodeConsumer(kafkaKonfigurasjon),
        besvarelseConsumer(kafkaKonfigurasjon),
        hendelseProducer(kafkaKonfigurasjon)
    ) { periodeConsumer, besvarelseConsumer, hendelseProducer ->
        val consumerStatus = StatusConsumerRebalanceListener(
            kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic,
            kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic
        )
        periodeConsumer.subscribe(consumerStatus, kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic)
        besvarelseConsumer.subscribe(consumerStatus, kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic)
        val utfoertAv = Bruker(
            type = BrukerType.SYSTEM,
            id = applikasjonKonfigurasjon.applicationName
        )
        val perioder: Sequence<Pair<Boolean, List<Hendelse>>> = periodeConsumer.asSequence(
            avslutt = avslutt,
            mapper = (::tilPeriode)(utfoertAv)
        ).map { periodeBatch -> consumerStatus.erKlar(periodeConsumer.subscription()) to periodeBatch }
        val opplysninger: Sequence<Pair<Boolean, List<Hendelse>>> = besvarelseConsumer.asSequence(
            avslutt = avslutt,
            mapper = (::situasjonMottat)(utfoertAv)
        ).map { situasjonBatch -> consumerStatus.erKlar(besvarelseConsumer.subscription()) to situasjonBatch }

        val tomBatchTeller = AtomicLong(0)
        perioder.zip(opplysninger)
            .map { (perioder, situasjoner) -> (perioder.first && situasjoner.first) to (perioder.second + situasjoner.second) }
            .forEach { (erKlar, hendelser) ->
                //Litt deffensiv kode for å unngå at vi begynner å sende data til topic før vi er sikre på vi har konsumeret alle hendelser fra topics
                //Feks ved rebalansering vil vi få en tom batch selv om consumeren ligger langt bak,
                // "erKlar" skal indikere om alle topics aktivt konsumerer hendelser
                val antallTommeBatcher = if (hendelser.isEmpty()) {
                    tomBatchTeller.incrementAndGet()
                } else {
                    tomBatchTeller.set(0)
                    0L
                }
                when {
                    erKlar && antallTommeBatcher > 3L -> {
                        loggTid("Last og send batch til topic") {
                            skrivTilTopic(
                                kafkaKonfigurasjon.klientKonfigurasjon.eventlogTopic,
                                hendelseProducer,
                                dependencies.kafkaKeysClient
                            )
                        }
                    }

                    hendelser.isNotEmpty() -> {
                        loggTid("Skriv batch til db[størrelse=${hendelser.size}]") {
                            skrivBatchTilDb(serializer = hendelseTilBytes, batch = hendelser)
                        }
                    }

                    else -> {
                        if (tomBatchTeller.get() % 30 == 0L) {
                            logger.info("Venter på at alle topics skal være klare")
                        }
                    }
                }
            }
    }
}
