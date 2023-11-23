package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.GJELDER_FRA_DATO
import no.nav.paw.arbeidssokerregisteret.GJELDER_TIL_DATO
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.DinSituasjonSvar
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.besvarelse.UtdanningSvar
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.Metadata


fun tilPeriode(periode: ArbeidssokerperiodeHendelseMelding): Hendelse =
    when (periode.hendelse) {
        no.nav.paw.migrering.Hendelse.STARTET -> periode.toStartEvent()
        no.nav.paw.migrering.Hendelse.STOPPET -> periode.toAvsluttetEvent()
    }

fun tilSituasjonElement(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent): ArbeidssoekersitusjonMedDetaljer? =
    when (arbeidssokerBesvarelseEvent.besvarelse.dinSituasjon.verdi) {
        DinSituasjonSvar.MISTET_JOBBEN -> ArbeidsoekersituasjonBeskrivelse.HAR_BLITT_SAGT_OPP
        DinSituasjonSvar.OPPSIGELSE -> ArbeidsoekersituasjonBeskrivelse.HAR_SAGT_OPP
        DinSituasjonSvar.HAR_SAGT_OPP -> ArbeidsoekersituasjonBeskrivelse.HAR_SAGT_OPP
        DinSituasjonSvar.SAGT_OPP -> ArbeidsoekersituasjonBeskrivelse.HAR_BLITT_SAGT_OPP
        DinSituasjonSvar.ALDRI_HATT_JOBB -> ArbeidsoekersituasjonBeskrivelse.ALDRI_HATT_JOBB
        DinSituasjonSvar.VIL_BYTTE_JOBB -> ArbeidsoekersituasjonBeskrivelse.VIL_BYTTE_JOBB
        DinSituasjonSvar.ER_PERMITTERT -> ArbeidsoekersituasjonBeskrivelse.ER_PERMITTERT
        DinSituasjonSvar.USIKKER_JOBBSITUASJON -> ArbeidsoekersituasjonBeskrivelse.USIKKER_JOBBSITUASJON
        DinSituasjonSvar.JOBB_OVER_2_AAR -> ArbeidsoekersituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        DinSituasjonSvar.VIL_FORTSETTE_I_JOBB -> ArbeidsoekersituasjonBeskrivelse.ANNET
        DinSituasjonSvar.AKKURAT_FULLFORT_UTDANNING -> ArbeidsoekersituasjonBeskrivelse.AKKURAT_FULLFORT_UTDANNING
        DinSituasjonSvar.DELTIDSJOBB_VIL_MER -> ArbeidsoekersituasjonBeskrivelse.DELTIDSJOBB_VIL_MER
        DinSituasjonSvar.ENDRET_PERMITTERINGSPROSENT -> ArbeidsoekersituasjonBeskrivelse.ER_PERMITTERT
        DinSituasjonSvar.TILBAKE_TIL_JOBB -> null
        DinSituasjonSvar.NY_JOBB -> ArbeidsoekersituasjonBeskrivelse.NY_JOBB
        DinSituasjonSvar.MIDLERTIDIG_JOBB -> ArbeidsoekersituasjonBeskrivelse.MIDLERTIDIG_JOBB
        DinSituasjonSvar.KONKURS -> ArbeidsoekersituasjonBeskrivelse.KONKURS
        DinSituasjonSvar.UAVKLART -> ArbeidsoekersituasjonBeskrivelse.USIKKER_JOBBSITUASJON
        DinSituasjonSvar.ANNET -> ArbeidsoekersituasjonBeskrivelse.ANNET
        null -> null
    }?.let { beskrivelse ->
        ArbeidssoekersitusjonMedDetaljer(
            beskrivelse = beskrivelse,
            detaljer = mapOf(
                GJELDER_FRA_DATO to arbeidssokerBesvarelseEvent.besvarelse?.dinSituasjon?.gjelderFraDato?.toIso8601(),
                GJELDER_TIL_DATO to arbeidssokerBesvarelseEvent.besvarelse?.dinSituasjon?.gjelderTilDato?.toIso8601(),
                PROSENT to listOfNotNull(
                    arbeidssokerBesvarelseEvent.besvarelse?.dinSituasjon?.tilleggsData?.permitteringsProsent,
                    arbeidssokerBesvarelseEvent.besvarelse?.dinSituasjon?.tilleggsData?.stillingsProsent
                ).firstOrNull()
            ).mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
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
        UtdanningSvar.VIDEREGAENDE_FAGBREV_SVENNEBREV -> Utdanningsnivaa.VIDEREGAENDE_FAGUTDANNING_SVENNEBREV
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
    SituasjonMottatt(
        hendelseId = UUID.randomUUID(),
        identitetsnummer = arbeidssokerBesvarelseEvent.foedselsnummer,
        situasjon = Situasjon(
            id = UUID.randomUUID(),
            metadata = Metadata(
                arbeidssokerBesvarelseEvent.registreringsTidspunkt.truncatedTo(ChronoUnit.MILLIS),
                Bruker(
                    BrukerType.SLUTTBRUKER,
                    arbeidssokerBesvarelseEvent.foedselsnummer
                ),
                "veilarbregistrering",
                "overføring"
            ),
            utdanning = Utdanning(
                utdanningsnivaa = utdanningsnivaa(arbeidssokerBesvarelseEvent),
                bestaatt = jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.utdanningBestatt.verdi.name),
                godkjent = jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.utdanningGodkjent.verdi.name)
            ),
            helse = Helse(
                jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.helseHinder.verdi.name)
            ),
            arbeidserfaring = arbeidserfaring(arbeidssokerBesvarelseEvent),
            arbeidsoekersituasjon = Arbeidsoekersituasjon(
                listOfNotNull(
                    tilSituasjonElement(arbeidssokerBesvarelseEvent)
                )
            )
        )
    )
