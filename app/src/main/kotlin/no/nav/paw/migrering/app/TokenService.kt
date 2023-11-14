package no.nav.paw.migrering.app

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.paw.migrering.app.konfigurasjon.AzureConfig
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class TokenService(config: AzureConfig) {
    fun createMachineToMachineToken(scope: String): String {
        return aadMachineToMachineTokenClient.createMachineToMachineToken(scope)
    }

    private val aadMachineToMachineTokenClient = when (System.getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp", "dev-gcp" -> AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.clientId)
            .withPrivateJwk(createMockRSAKey("azure"))
            .withTokenEndpointUrl(config.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()
    }
}

fun createMockRSAKey(keyID: String): String? = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
            .toJSONString()
    }
