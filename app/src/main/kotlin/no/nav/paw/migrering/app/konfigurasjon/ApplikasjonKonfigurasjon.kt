package no.nav.paw.migrering.app.konfigurasjon

data class ApplikasjonKonfigurasjon(
    val azureConfig: AzureConfig,
    val kafkaKeysConfig: ServiceClientConfig
)

data class AzureConfig(
    val clientId: String,
    val tokenEndpointUrl: String
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)
