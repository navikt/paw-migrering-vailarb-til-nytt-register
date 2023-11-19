package no.nav.paw.migrering.app

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

fun createDependencies(config: ApplikasjonKonfigurasjon): Dependencies {
    val kafkaKeysClient = when (config.kafkaKeysConfig.url) {
        "MOCK" -> inMemoryKafkaKeysMock()
        else -> kafkaKeysKlient(config)
    }
    return Dependencies(
        kafkaKeysClient = kafkaKeysClient
    )
}

private fun kafkaKeysKlient(config: ApplikasjonKonfigurasjon): KafkaKeysClient {
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

fun inMemoryKafkaKeysMock(): KafkaKeysClient {
    val naisClusterName = System.getenv("NAIS_CLUSTER_NAME")
    if (naisClusterName != null) {
        throw IllegalStateException("Kan ikke bruke inMemoryKafkaKeysMock i $naisClusterName")
    }
    val sekvens = AtomicLong(0)
    val map: ConcurrentMap<String, Long> = ConcurrentHashMap()
    return object: KafkaKeysClient {
        override suspend fun getKey(identitetsnummer: String): KafkaKeysResponse {
            return KafkaKeysResponse(map.computeIfAbsent(identitetsnummer) { sekvens.incrementAndGet() })
        }
    }
}

data class Dependencies(
    val kafkaKeysClient: KafkaKeysClient
)
