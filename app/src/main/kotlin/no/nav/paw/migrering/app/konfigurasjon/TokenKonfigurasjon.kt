package no.nav.paw.migrering.app.konfigurasjon

data class TokenKonfigurasjon(
    val tokenEndpointUrl: String,
    val scope: String,
    val clientId: String
)
