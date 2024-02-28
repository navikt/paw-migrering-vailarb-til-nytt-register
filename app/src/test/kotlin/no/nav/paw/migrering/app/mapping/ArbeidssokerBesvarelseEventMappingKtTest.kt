package no.nav.paw.migrering.app.mapping

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.besvarelse.*
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

class ArbeidssokerBesvarelseEventMappingKtTest : FreeSpec({
    "Når personen ikke har hatt jobb skal vi legge til 'ALDRI_HATT_JOBB' i hendelsen" {
        val veilarbEvent = event(SisteStillingSvar.HAR_IKKE_HATT_JOBB)
        val event = situasjonMottat(bruker, veilarbEvent)
        val beskrivelser = event.opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser
        beskrivelser.size shouldBe 2
        beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.ER_PERMITTERT }
            .shouldNotBeNull()
        beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB }
            .shouldNotBeNull()
    }
    "Når personen har hatt jobb skal vi ikke legge til 'ALDRI_HATT_JOBB' i hendelsen" {
        val veilarbEvent = event(SisteStillingSvar.HAR_HATT_JOBB)
        val event = situasjonMottat(bruker, veilarbEvent)
        val beskrivelser = event.opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser
        beskrivelser.size shouldBe 1
        beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.ER_PERMITTERT }
            .shouldNotBeNull()
    }
    "Når svaret er 'INGEN_SVAR' skal vi ikke legge til 'ALDRI_HATT_JOBB' i hendelsen" {
        val veilarbEvent = event(SisteStillingSvar.INGEN_SVAR)
        val event = situasjonMottat(bruker, veilarbEvent)
        val beskrivelser = event.opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser
        beskrivelser.size shouldBe 1
        beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.ER_PERMITTERT }
            .shouldNotBeNull()
    }
})

val bruker = Bruker(
    BrukerType.SYSTEM,
    "junit"
)

fun event(harHattJobb: SisteStillingSvar): ArbeidssokerBesvarelseEvent =
    ArbeidssokerBesvarelseEvent(
        Random.nextInt(),
        Random.nextInt(),
        "123456789801",
        "12345678901",
        Instant.now(),
        Instant.now(),
        OpprettetAv.SYSTEM,
        EndretAv.SYSTEM,
        false,
        Besvarelse(
            Utdanning(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                UtdanningSvar.INGEN_UTDANNING
            ),
            UtdanningBestatt(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                UtdanningBestattSvar.JA
            ),
            UtdanningGodkjent(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                UtdanningGodkjentSvar.JA
            ),
            HelseHinder(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                HelseHinderSvar.JA
            ),
            AndreForhold(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                AndreForholdSvar.JA
            ),
            SisteStilling(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                harHattJobb
            ),
            DinSituasjon(
                Instant.now(),
                "junit",
                LocalDate.now(),
                LocalDate.now(),
                DinSituasjonSvar.ER_PERMITTERT,
                DinSituasjonTilleggsData(
                    LocalDate.now(),
                    LocalDate.now(),
                    LocalDate.now(),
                    LocalDate.now(),
                    "50",
                    null,
                    "NEI",
                    "NEI"
                )
            ),
        )
    )
