package no.nav.paw.migrering.app

import no.nav.paw.migrering.app.kafkakeys.KafkaKeysClient
import no.nav.paw.migrering.app.kafkakeys.kafkaKeysKlient
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.DatabaseKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.dataSource
import javax.sql.DataSource

data class Dependencies(
    val kafkaKeysClient: KafkaKeysClient,
    val dataSource: DataSource
)

fun createDependencies(config: ApplikasjonKonfigurasjon, databaseKonfigurasjon: DatabaseKonfigurasjon): Dependencies {
    val kafkaKeysClient = kafkaKeysKlient(config)
    return Dependencies(
        kafkaKeysClient = kafkaKeysClient,
        dataSource = databaseKonfigurasjon.dataSource()
    )
}
