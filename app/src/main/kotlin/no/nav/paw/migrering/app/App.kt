package no.nav.paw.migrering.app

import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.db.skrivBatchTilDb
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.DatabaseKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.dataSource
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse as InternHendelse

fun main() {
    val logger = LoggerFactory.getLogger("migrering")
    val kafkaKonfigurasjon: KafkaKonfigurasjon = lastKonfigurasjon("kafka_konfigurasjon.toml")
    val applikasjonKonfigurasjon: ApplikasjonKonfigurasjon = lastKonfigurasjon("applikasjon_konfigurasjon.toml")
    val databaseKonfigurasjon: DatabaseKonfigurasjon = lastKonfigurasjon("postgres.toml")
    val dataSource = databaseKonfigurasjon.dataSource()
    flywayMigrate(dataSource)
    Database.connect(dataSource)
    val dependencies = createDependencies(applikasjonKonfigurasjon)

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
        periodeConsumer.subscribe(listOf(kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic))
        besvarelseConsumer.subscribe(listOf(kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic))
        val periodeSekvens: Sequence<List<InternHendelse>> = periodeConsumer.asSequence(avslutt, ::tilPeriode)
        val besvarelseSekvens: Sequence<List<InternHendelse>> = besvarelseConsumer.asSequence(avslutt) { it.tilSituasjonMottat() }
        periodeConsumer.use {
            periodeSekvens
                .zip(besvarelseSekvens)
                .map { (perioder, besvarelse) -> perioder + besvarelse }
                .forEach { hendelser ->
                    if (hendelser.isEmpty()) {
                        loggTid("Last og send batch til topic") {
                            skrivTilTopic(
                                kafkaKonfigurasjon.klientKonfigurasjon.eventlogTopic,
                                hendelseProducer,
                                dependencies.kafkaKeysClient
                            )
                        }
                    } else {
                        loggTid("Skriv batch til db[størrelse=${hendelser.size}]") {
                            skrivBatchTilDb(hendelser)
                        }
                    }
                }
        }
    }
}
