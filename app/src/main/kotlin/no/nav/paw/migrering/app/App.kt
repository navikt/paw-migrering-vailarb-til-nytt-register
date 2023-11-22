package no.nav.paw.migrering.app

import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.db.skrivBatchTilDb
import no.nav.paw.migrering.app.konfigurasjon.*
import no.nav.paw.migrering.app.serde.hendelseSerde
import no.nav.paw.migrering.app.serde.hendelseTilBytes
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse as InternHendelse

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
        periodeConsumer.subscribe(kafkaKonfigurasjon.klientKonfigurasjon.periodeTopic)
        besvarelseConsumer.subscribe(kafkaKonfigurasjon.klientKonfigurasjon.situasjonTopic)
        val periodeSekvens: Sequence<List<InternHendelse>> = periodeConsumer.asSequence(avslutt, ::tilPeriode)
        val besvarelseSekvens: Sequence<List<InternHendelse>> = besvarelseConsumer.asSequence(avslutt, ::situasjonMottat)
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
                        skrivBatchTilDb(serializer = hendelseTilBytes, batch = hendelser)
                    }
                }
            }
    }
}
