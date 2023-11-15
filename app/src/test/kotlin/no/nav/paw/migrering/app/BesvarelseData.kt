package no.nav.paw.migrering.app

import no.nav.paw.besvarelse.*
import java.time.Instant
import java.time.LocalDate

val besvarelser: List<ArbeidssokerBesvarelseEvent> = listOf(
    "12345678901" to "2002-05-29T15:21:08+02:00",
    "12345678902" to "2003-05-29T15:22:08+02:00",
    "12345678901" to "2008-05-29T15:23:08+02:00",
    "12345678902" to "2003-07-29T15:24:08+02:00",
    "12345678901" to "2011-06-29T15:24:08+02:00"
).map { (identitetsnummer, tidspunkt) ->
    lagHendelse(
        identitetsnummer,
        Instant.parse(tidspunkt)
    )
}

fun lagHendelse(
    identitetsnummer: String,
    tidspunkt: Instant,
) =
    ArbeidssokerBesvarelseEvent(
        1,
        2,
        identitetsnummer,
        identitetsnummer,
        tidspunkt,
        tidspunkt,
        OpprettetAv.BRUKER,
        EndretAv.BRUKER,
        true,
        Besvarelse(
            Utdanning(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                UtdanningSvar.HOYERE_UTDANNING_1_TIL_4
            ),
            UtdanningBestatt(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                UtdanningBestattSvar.JA
            ),
            UtdanningGodkjent(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                UtdanningGodkjentSvar.JA
            ),
            HelseHinder(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                HelseHinderSvar.NEI
            ),
            AndreForhold(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                AndreForholdSvar.NEI
            ),
            SisteStilling(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                SisteStillingSvar.HAR_HATT_JOBB
            ),
            DinSituasjon(
                Instant.parse("2001-06-29T15:24:08+02:00"),
                "noen",
                LocalDate.now(),
                LocalDate.now(),
                DinSituasjonSvar.MISTET_JOBBEN,
                DinSituasjonTilleggsData(
                    LocalDate.now(),
                    LocalDate.now(),
                    LocalDate.now(),
                    LocalDate.now(),
                    "45",
                    null,
                    "nei",
                    "nei",
                )
            ),
        )

    )
