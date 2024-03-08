package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.besvarelse.UtdanningSvar
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

object Nus {
    val ingenUtdanning = "0"
    val barneskole = "1"
    val ungdomsskole = "2"
    val videregaendeGrunnutdanning = "3"
    val videregaendeAvsluttende = "4"
    val paabyggingTilVideregaende = "5"
    val univeristetHoeyskoleLavereNivaa = "6"
    val univeristetHoeyskoleHoyereNivaa = "7"
    val forskerUtdanning = "8"
    val ikkeOppgitt = "9"
}

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

fun jaNeiVetIkke(navn: String): JaNeiVetIkke? =
    when (navn) {
        "JA" -> JaNeiVetIkke.JA
        "NEI" -> JaNeiVetIkke.NEI
        "VET_IKKE" -> JaNeiVetIkke.VET_IKKE
        else -> null
    }

fun nonNullableJaNeiVetIkke(navn: String): JaNeiVetIkke =
    when (navn) {
        "JA" -> JaNeiVetIkke.JA
        "NEI" -> JaNeiVetIkke.NEI
        "INGEN_SVAR" -> JaNeiVetIkke.VET_IKKE
        "VET_IKKE" -> JaNeiVetIkke.VET_IKKE
        else -> throw IllegalArgumentException("Ukjent verdi for JaNeiVetIkke: $navn")
    }

fun utdanningsnivaa(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    when (arbeidssokerBesvarelseEvent.besvarelse.utdanning.verdi) {
        UtdanningSvar.INGEN_UTDANNING -> Nus.ingenUtdanning
        UtdanningSvar.GRUNNSKOLE -> Nus.barneskole
        UtdanningSvar.VIDEREGAENDE_GRUNNUTDANNING -> Nus.videregaendeGrunnutdanning
        UtdanningSvar.VIDEREGAENDE_FAGBREV_SVENNEBREV -> Nus.videregaendeAvsluttende
        UtdanningSvar.HOYERE_UTDANNING_1_TIL_4 -> Nus.univeristetHoeyskoleLavereNivaa
        UtdanningSvar.HOYERE_UTDANNING_5_ELLER_MER -> Nus.univeristetHoeyskoleHoyereNivaa
        UtdanningSvar.INGEN_SVAR -> Nus.ikkeOppgitt
        null -> Nus.ikkeOppgitt
    }

fun arbeidserfaring(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    when (arbeidssokerBesvarelseEvent.besvarelse.sisteStilling.verdi) {
        SisteStillingSvar.HAR_HATT_JOBB -> null
        SisteStillingSvar.HAR_IKKE_HATT_JOBB -> JobbsituasjonMedDetaljer(
            beskrivelse = JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB,
            detaljer = emptyMap()
        )

        SisteStillingSvar.INGEN_SVAR -> null
        null -> null
    }

fun situasjonMottat(id: Long, utfoertAv: Bruker, arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent) =
    OpplysningerOmArbeidssoekerMottatt(
        hendelseId = UUID.randomUUID(),
        id = id,
        identitetsnummer = arbeidssokerBesvarelseEvent.foedselsnummer,
        opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
            id = UUID.randomUUID(),
            metadata = Metadata(
                tidspunkt = extractTimestamp(arbeidssokerBesvarelseEvent),
                utfoertAv = utfoertAv,
                kilde = "veilarbregistrering",
                aarsak = "overføring"
            ),
            utdanning = Utdanning(
                nus = utdanningsnivaa(arbeidssokerBesvarelseEvent),
                bestaatt = jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.utdanningBestatt.verdi.name),
                godkjent = jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.utdanningGodkjent.verdi.name)
            ),
            helse = Helse(
                nonNullableJaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.helseHinder.verdi.name)
            ),
            jobbsituasjon = Jobbsituasjon(
                listOfNotNull(
                    tilSituasjonElement(arbeidssokerBesvarelseEvent),
                    arbeidserfaring(arbeidssokerBesvarelseEvent),
                )
            ),
            annet = Annet(
                jaNeiVetIkke(arbeidssokerBesvarelseEvent.besvarelse.andreForhold.verdi.name)
            )
        )
    )

private fun extractTimestamp(arbeidssokerBesvarelseEvent: ArbeidssokerBesvarelseEvent): Instant {
    val timestamp = if (arbeidssokerBesvarelseEvent.endret) {
        arbeidssokerBesvarelseEvent.endretTidspunkt.truncatedTo(ChronoUnit.MILLIS)
    } else {
        arbeidssokerBesvarelseEvent.registreringsTidspunkt.truncatedTo(ChronoUnit.MILLIS)
    }
    val timeZone = TimeZone.getTimeZone("Europe/Oslo")
    val correctTimestamp = if (timeZone.inDaylightTime(Date.from(timestamp))) {
        timestamp.plus(Duration.ofHours(1))
    } else {
        timestamp
    }
    return conditionallyAddOneMilliSecond(correctTimestamp)
}
