package no.nav.paw.migrering.app.konfigurasjon

data class SchemaRegistryKonfigurasjon(
    val url: String,
    val bruker: String?,
    val passord: String?,
    val autoRegistrerSchema: Boolean = true)

