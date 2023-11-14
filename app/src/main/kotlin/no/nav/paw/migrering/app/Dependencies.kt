package no.nav.paw.migrering.app

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon

fun createDependencies(config: ApplikasjonKonfigurasjon): Dependencies {
    val tokenService = TokenService(config.azureConfig)
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }
    val kafkaKeysClient = StandardKafkaKeysClient(
        httpClient,
        config.kafkaKeysConfig.url
    ) { tokenService.createMachineToMachineToken(config.kafkaKeysConfig.scope) }

    return Dependencies(
        kafkaKeysClient
    )
}

data class Dependencies(
    val kafkaKeysClient: KafkaKeysClient
)
