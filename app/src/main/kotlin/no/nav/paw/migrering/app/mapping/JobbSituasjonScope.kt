package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent

interface JobbSituasjonScope<V> {
    val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse
    fun detaljer(kilde: V): Map<String, String?>
}

object HarBlittSagtOppScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> {
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP
    override fun detaljer(kilde: ArbeidssokerBesvarelseEvent): Map<String, String?> = mapOf(
        GJELDER_FRA_DATO to kilde.besvarelse?.dinSituasjon?.tilleggsData?.oppsigelseDato?.toIso8601(),
        SISTE_DAG_MED_LOENN to kilde.besvarelse?.dinSituasjon?.tilleggsData?.sisteArbeidsdagDato?.toIso8601()
    )
}

object HarSagtOppScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> by HarBlittSagtOppScope{
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.HAR_SAGT_OPP
}

object KonkursScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> by HarBlittSagtOppScope{
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.KONKURS
}

object PermiteringsScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> {
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT
    override fun detaljer(kilde: ArbeidssokerBesvarelseEvent): Map<String, String?> = mapOf(
        GJELDER_FRA_DATO to kilde.besvarelse?.dinSituasjon?.tilleggsData?.gjelderFraDato?.toIso8601(),
        GJELDER_TIL_DATO to kilde.besvarelse?.dinSituasjon?.gjelderTilDato?.toIso8601(),
        PROSENT to kilde.besvarelse?.dinSituasjon?.tilleggsData?.permitteringsProsent?.toString()
    )
}

object NyJobbScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> {
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.NY_JOBB
    override fun detaljer(kilde: ArbeidssokerBesvarelseEvent): Map<String, String?> = mapOf(
        GJELDER_FRA_DATO to kilde.besvarelse?.dinSituasjon?.tilleggsData?.forsteArbeidsdagDato?.toIso8601()
    )
}

object DeltidsJobbVilMerScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> {
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER
    override fun detaljer(kilde: ArbeidssokerBesvarelseEvent): Map<String, String?> = mapOf(
        GJELDER_FRA_DATO to kilde.besvarelse?.dinSituasjon?.gjelderFraDato?.toIso8601(),
        PROSENT to kilde.besvarelse?.dinSituasjon?.tilleggsData?.stillingsProsent
    )
}

class StandardScope(
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse,
): JobbSituasjonScope<ArbeidssokerBesvarelseEvent> {
    override fun detaljer(kilde: ArbeidssokerBesvarelseEvent): Map<String, String?> = mapOf(
        GJELDER_FRA_DATO to kilde.besvarelse?.dinSituasjon?.gjelderFraDato?.toIso8601()
    )
}

/**
 * Typisk brukt når vi bare skal vise at det ikke er noen speisell situasjon for øyeblikket.
 */
object UdefinertScope: JobbSituasjonScope<ArbeidssokerBesvarelseEvent> {
    override val jobSituasjonBeskrivelse: JobbsituasjonBeskrivelse = JobbsituasjonBeskrivelse.UDEFINERT
    override fun detaljer(kilde: ArbeidssokerBesvarelseEvent): Map<String, String?> = emptyMap()
}
