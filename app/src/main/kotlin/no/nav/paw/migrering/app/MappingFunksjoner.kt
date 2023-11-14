package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.GJELDER_FRA_DATO
import no.nav.paw.arbeidssokerregisteret.GJELDER_TIL_DATO
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.intern.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.intern.v1.Arbeidsoekersituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.Element
import no.nav.paw.arbeidssokerregisteret.intern.v1.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanning
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanningsnivaa
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.DinSituasjonSvar
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.besvarelse.UtdanningSvar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun ArbeidssokerBesvarelseEvent.tilSituasjonMottat(): SituasjonMottat = situasjonMottat(this)
fun tilSituasjonElement(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent): Element? =
    when (arbeidssokerBesvarelseEvent.besvarelse.dinSituasjon.verdi) {
        DinSituasjonSvar.MISTET_JOBBEN -> Beskrivelse.HAR_BLITT_SAGT_OPP
        DinSituasjonSvar.OPPSIGELSE -> Beskrivelse.HAR_SAGT_OPP
        DinSituasjonSvar.HAR_SAGT_OPP -> Beskrivelse.HAR_SAGT_OPP
        DinSituasjonSvar.SAGT_OPP -> Beskrivelse.HAR_BLITT_SAGT_OPP
        DinSituasjonSvar.ALDRI_HATT_JOBB -> Beskrivelse.ALDRI_HATT_JOBB
        DinSituasjonSvar.VIL_BYTTE_JOBB -> Beskrivelse.VIL_BYTTE_JOBB
        DinSituasjonSvar.ER_PERMITTERT -> Beskrivelse.ER_PERMITTERT
        DinSituasjonSvar.USIKKER_JOBBSITUASJON -> Beskrivelse.USIKKER_JOBBSITUASJON
        DinSituasjonSvar.JOBB_OVER_2_AAR -> Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        DinSituasjonSvar.VIL_FORTSETTE_I_JOBB -> Beskrivelse.ANNET
        DinSituasjonSvar.AKKURAT_FULLFORT_UTDANNING -> Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        DinSituasjonSvar.DELTIDSJOBB_VIL_MER -> Beskrivelse.DELTIDSJOBB_VIL_MER
        DinSituasjonSvar.ENDRET_PERMITTERINGSPROSENT -> Beskrivelse.ER_PERMITTERT
        DinSituasjonSvar.TILBAKE_TIL_JOBB -> null
        DinSituasjonSvar.NY_JOBB -> Beskrivelse.NY_JOBB
        DinSituasjonSvar.MIDLERTIDIG_JOBB -> Beskrivelse.MIDLERTIDIG_JOBB
        DinSituasjonSvar.KONKURS -> Beskrivelse.KONKURS
        DinSituasjonSvar.UAVKLART -> Beskrivelse.USIKKER_JOBBSITUASJON
        DinSituasjonSvar.ANNET -> Beskrivelse.ANNET
        null -> null
    }?.let { beskrivelse ->
        Element(
            beskrivelse,
            mapOf(
                GJELDER_FRA_DATO to arbeidssokerBesvarelseEvent.besvarelse.dinSituasjon.gjelderFraDato.toIso8601(),
                GJELDER_TIL_DATO to arbeidssokerBesvarelseEvent.besvarelse.dinSituasjon.gjelderTilDato.toIso8601(),
                PROSENT to listOfNotNull(
                    arbeidssokerBesvarelseEvent.besvarelse.dinSituasjon.tilleggsData.permitteringsProsent,
                    arbeidssokerBesvarelseEvent.besvarelse.dinSituasjon.tilleggsData.stillingsProsent
                ).firstOrNull()
            ).filterValues { it != null }
        )
    }

fun LocalDate.toIso8601(): String = this.format(DateTimeFormatter.ISO_DATE)

fun jaNeiVetIkke(navn: String) =
    when (navn) {
        "JA" -> JaNeiVetIkke.JA
        "NEI" -> JaNeiVetIkke.NEI
        "INGEN_SVAR" -> JaNeiVetIkke.VET_IKKE
        "VET_IKKE" -> JaNeiVetIkke.VET_IKKE
        else -> throw IllegalArgumentException("Ukjent verdi for JaNeiVetIkke: $navn")
    }

fun utdanningsnivaa(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    when (arbeidssokerBesvarelseEvent.besvarelse.utdanning.verdi) {
        UtdanningSvar.INGEN_UTDANNING -> Utdanningsnivaa.INGEN_UTDANNING
        UtdanningSvar.GRUNNSKOLE -> Utdanningsnivaa.GRUNNSKOLE
        UtdanningSvar.VIDEREGAENDE_GRUNNUTDANNING -> Utdanningsnivaa.VIDEREGAENDE_GRUNNUTDANNING
        UtdanningSvar.VIDEREGAENDE_FAGBREV_SVENNEBREV -> Utdanningsnivaa.VIDEREGAENDE_FAGBREV_SVENNEBREV
        UtdanningSvar.HOYERE_UTDANNING_1_TIL_4 -> Utdanningsnivaa.HOYERE_UTDANNING_1_TIL_4
        UtdanningSvar.HOYERE_UTDANNING_5_ELLER_MER -> Utdanningsnivaa.HOYERE_UTDANNING_5_ELLER_MER
        UtdanningSvar.INGEN_SVAR -> Utdanningsnivaa.UDEFINERT
        null -> Utdanningsnivaa.UDEFINERT
    }

fun arbeidserfaring(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    Arbeidserfaring(
        when (arbeidssokerBesvarelseEvent.besvarelse.sisteStilling.verdi) {
            SisteStillingSvar.HAR_HATT_JOBB -> JaNeiVetIkke.JA
            SisteStillingSvar.HAR_IKKE_HATT_JOBB -> JaNeiVetIkke.NEI
            SisteStillingSvar.INGEN_SVAR -> JaNeiVetIkke.VET_IKKE
            null -> JaNeiVetIkke.VET_IKKE
        }
    )

fun situasjonMottat(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    SituasjonMottat(
        arbeidssokerBesvarelseEvent.foedselsnummer,
        Metadata(
            arbeidssokerBesvarelseEvent.registreringsTidspunkt,
            Bruker(
                BrukerType.SLUTTBRUKER,
                arbeidssokerBesvarelseEvent.foedselsnummer
            ),
            "veilarbregistrering",
            "overføring"
        ),
        Utdanning(
            utdanningsnivaa(arbeidssokerBesvarelseEvent),
            jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.utdanningBestatt.verdi.name),
            jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.utdanningGodkjent.verdi.name)
        ),
        Helse(
            jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.helseHinder.verdi.name)
        ),
        arbeidserfaring(arbeidssokerBesvarelseEvent),
        Arbeidsoekersituasjon(
            listOfNotNull(
                tilSituasjonElement(arbeidssokerBesvarelseEvent)
            )
        )
    )
