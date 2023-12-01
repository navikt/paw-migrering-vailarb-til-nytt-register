package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.besvarelse.UtdanningSvar
import java.time.temporal.ChronoUnit
import java.util.*


fun tilSituasjonElement(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent): JobbsituasjonMedDetaljer? =
    with(jobbsituasjonScope(arbeidssokerBesvarelseEvent.besvarelse?.dinSituasjon?.verdi)) {
        jobbSituasjonMedDetaljer(arbeidssokerBesvarelseEvent).copy(
            beskrivelse = jobSituasjonBeskrivelse,
            detaljer = detaljer(arbeidssokerBesvarelseEvent)
                .mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        )
    }

context(JobbSituasjonScope<ArbeidssokerBesvarelseEvent>)
fun jobbSituasjonMedDetaljer(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) = JobbsituasjonMedDetaljer(
    beskrivelse = jobSituasjonBeskrivelse,
    detaljer = detaljer(arbeidssokerBesvarelseEvent)
        .mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
)

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

fun situasjonMottat(utfoertAv: Bruker, arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    OpplysningerOmArbeidssoekerMottatt(
        hendelseId = UUID.randomUUID(),
        identitetsnummer = arbeidssokerBesvarelseEvent.foedselsnummer,
        opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
            id = UUID.randomUUID(),
            metadata = Metadata(
                tidspunkt = arbeidssokerBesvarelseEvent.registreringsTidspunkt.truncatedTo(ChronoUnit.MILLIS),
                utfoertAv = utfoertAv,
                kilde = "veilarbregistrering",
                aarsak = "overføring"
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
            jobbsituasjon = Jobbsituasjon(
                listOfNotNull(
                    tilSituasjonElement(arbeidssokerBesvarelseEvent)
                )
            )
        )
    )
