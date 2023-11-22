package no.nav.paw.migrering.app.kafkakeys

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.paw.migrering.app.KafkaKeysClient
import no.nav.paw.migrering.app.StandardKafkaKeysClient
import no.nav.paw.migrering.app.TokenService
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon


fun kafkaKeysKlient(konfigurasjon: ApplikasjonKonfigurasjon): KafkaKeysClient =
    when (konfigurasjon.kafkaKeysConfig.url) {
        "MOCK" -> inMemoryKafkaKeysMock()
        else -> kafkaKeysMedHttpClient(konfigurasjon)
    }

private fun kafkaKeysMedHttpClient(config: ApplikasjonKonfigurasjon): KafkaKeysClient {
    val tokenService = TokenService(config.azureConfig)
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }
    return StandardKafkaKeysClient(
        httpClient,
        config.kafkaKeysConfig.url
    ) { tokenService.createMachineToMachineToken(config.kafkaKeysConfig.scope) }
}
