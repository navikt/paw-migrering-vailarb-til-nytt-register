package no.nav.paw.migrering.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

data class KafkaKeysResponse(
    val id: Long
)

data class KafkaKeysRequest(
    val ident: String
)

interface KafkaKeysClient {
    suspend fun getKey(identitetsnummer: String): KafkaKeysResponse
}

class StandardKafkaKeysClient(
    private val httpClient: HttpClient,
    private val kafkaKeysUrl: String,
    private val getAccessToken: () -> String
) : KafkaKeysClient {
    override suspend fun getKey(identitetsnummer: String): KafkaKeysResponse =
        httpClient.post(kafkaKeysUrl) {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(ContentType.Application.Json)
            setBody(KafkaKeysRequest(identitetsnummer))
        }.body<KafkaKeysResponse>()
}
