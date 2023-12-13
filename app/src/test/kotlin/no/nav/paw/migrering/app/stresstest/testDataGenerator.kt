package no.nav.paw.migrering.app.stresstest

import no.nav.paw.besvarelse.DinSituasjon
import no.nav.paw.besvarelse.DinSituasjonTilleggsData
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.Hendelse
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

private val idSekvens = AtomicLong(0)

fun genererNyId(): String = idSekvens.incrementAndGet()
    .toString()
    .padStart(11, '0')

fun hentIder(antall: Int): List<String> = (1..antall)
    .map { genererNyId() }

fun periodeHendelse(
    hendelse: Hendelse,
    identitetsnummer: String,
    tidspunkt: LocalDateTime = LocalDateTime.now()
): ArbeidssokerperiodeHendelseMelding = ArbeidssokerperiodeHendelseMelding(
    hendelse = hendelse,
    foedselsnummer = identitetsnummer,
    tidspunkt = tidspunkt
)

fun besvarelse(
    identitetsnummer: String,
    tidspunkt: Instant = Instant.now()
) = no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent(
    /* id = */ Random.nextInt(),
    /* registreringsId = */ Random.nextInt(),
    /* foedselsnummer = */ identitetsnummer,
    /* aktorId = */ identitetsnummer,
    /* endretTidspunkt = */ tidspunkt,
    /* registreringsTidspunkt = */ tidspunkt,
    /* opprettetAv = */ no.nav.paw.besvarelse.OpprettetAv.BRUKER,
    /* endretAv = */ no.nav.paw.besvarelse.EndretAv.BRUKER,
    /* endret = */ true,
    /* besvarelse = */
    no.nav.paw.besvarelse.Besvarelse(
        no.nav.paw.besvarelse.Utdanning(
            Instant.now().minus(Duration.ofDays(256)),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            no.nav.paw.besvarelse.UtdanningSvar.HOYERE_UTDANNING_1_TIL_4
        ),
        no.nav.paw.besvarelse.UtdanningBestatt(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            no.nav.paw.besvarelse.UtdanningBestattSvar.JA
        ),
        no.nav.paw.besvarelse.UtdanningGodkjent(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            no.nav.paw.besvarelse.UtdanningGodkjentSvar.JA
        ),
        no.nav.paw.besvarelse.HelseHinder(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            no.nav.paw.besvarelse.HelseHinderSvar.NEI
        ),
        no.nav.paw.besvarelse.AndreForhold(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            no.nav.paw.besvarelse.AndreForholdSvar.NEI
        ),
        no.nav.paw.besvarelse.SisteStilling(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            SisteStillingSvar.INGEN_SVAR
        ),
        DinSituasjon(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            no.nav.paw.besvarelse.DinSituasjonSvar.MISTET_JOBBEN,
            DinSituasjonTilleggsData(
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                "12",
                null,
                "nei",
                null
            )
        )
    )
)
