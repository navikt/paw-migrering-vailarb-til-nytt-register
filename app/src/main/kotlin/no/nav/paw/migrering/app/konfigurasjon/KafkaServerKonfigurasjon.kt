package no.nav.paw.migrering.app.konfigurasjon

data class KafkaServerKonfigurasjon(
    val autentisering: String,
    val kafkaBrokers: String,
    val keystorePath: String?,
    val credstorePassword: String?,
    val truststorePath: String?
)
