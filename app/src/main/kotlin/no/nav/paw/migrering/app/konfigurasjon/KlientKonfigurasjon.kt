package no.nav.paw.migrering.app.konfigurasjon

data class KlientKonfigurasjon(
    val eventlogTopic: String,
    val periodeTopic: String,
    val situasjonTopic: String,
    val opplysningerFraVeilarbTopic: String,
    val maksHentetPerKall: Int = 2000,
    val konsumerGruppeId: String = "migrering_v1",
    val produsentKlientId: String = "migrering_v1"
)
