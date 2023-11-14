package no.nav.paw.migrering.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson

data class KafkaKeysResponse(
    val id: Long,
)

data class KafkaKeysRequest(
    val ident: String,
)

val httpClient = HttpClient {
    install(ContentNegotiation) {
        jackson()
    }
}

interface KafkaKeysClient {
    suspend fun getKey(identitetsnummer: String): KafkaKeysResponse
}

class StandardKafkaKeysClient(
    private val kafkaKeysUrl: String,
    private val getAccessToken: () -> String,
) : KafkaKeysClient {
    override suspend fun getKey(identitetsnummer: String): KafkaKeysResponse =
        httpClient.post(kafkaKeysUrl) {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(ContentType.Application.Json)
            setBody(KafkaKeysRequest(identitetsnummer))
        }.body<KafkaKeysResponse>()
}
