package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.DinSituasjonSvar

fun jobbsituasjonScope(dinSituasjonSvar: DinSituasjonSvar?): JobbSituasjonScope<ArbeidssokerBesvarelseEvent> = when (dinSituasjonSvar) {
    DinSituasjonSvar.MISTET_JOBBEN -> HarBlittSagtOppScope
    DinSituasjonSvar.OPPSIGELSE -> HarSagtOppScope
    DinSituasjonSvar.HAR_SAGT_OPP -> HarSagtOppScope
    DinSituasjonSvar.SAGT_OPP -> HarBlittSagtOppScope
    DinSituasjonSvar.ALDRI_HATT_JOBB -> StandardScope(JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB)
    DinSituasjonSvar.VIL_BYTTE_JOBB -> StandardScope(JobbsituasjonBeskrivelse.VIL_BYTTE_JOBB)
    DinSituasjonSvar.ER_PERMITTERT -> PermiteringsScope
    DinSituasjonSvar.USIKKER_JOBBSITUASJON -> StandardScope(JobbsituasjonBeskrivelse.USIKKER_JOBBSITUASJON)
    DinSituasjonSvar.JOBB_OVER_2_AAR -> StandardScope(JobbsituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR)
    DinSituasjonSvar.VIL_FORTSETTE_I_JOBB -> StandardScope(JobbsituasjonBeskrivelse.ANNET)
    DinSituasjonSvar.AKKURAT_FULLFORT_UTDANNING -> StandardScope(JobbsituasjonBeskrivelse.AKKURAT_FULLFORT_UTDANNING)
    DinSituasjonSvar.DELTIDSJOBB_VIL_MER -> DeltidsJobbVilMerScope
    DinSituasjonSvar.ENDRET_PERMITTERINGSPROSENT -> PermiteringsScope
    DinSituasjonSvar.TILBAKE_TIL_JOBB -> UdefinertScope
    DinSituasjonSvar.NY_JOBB -> NyJobbScope
    DinSituasjonSvar.MIDLERTIDIG_JOBB -> StandardScope(JobbsituasjonBeskrivelse.MIDLERTIDIG_JOBB)
    DinSituasjonSvar.KONKURS -> KonkursScope
    DinSituasjonSvar.UAVKLART -> StandardScope(JobbsituasjonBeskrivelse.USIKKER_JOBBSITUASJON)
    DinSituasjonSvar.ANNET -> StandardScope(JobbsituasjonBeskrivelse.ANNET)
    null -> UdefinertScope
}
