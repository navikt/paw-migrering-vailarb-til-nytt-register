package no.nav.paw.migrering.app.konfigurasjon

data class ApplikasjonKonfigurasjon(
    val applicationName : String,
    val azureConfig: AzureConfig,
    val kafkaKeysConfig: KafkaKeysKlientKonfigurasjon
)

data class AzureConfig(
    val clientId: String,
    val tokenEndpointUrl: String
)

data class KafkaKeysKlientKonfigurasjon(
    val url: String,
    val scope: String
)
