package no.nav.paw.migrering.app

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.GJELDER_FRA_DATO
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.besvarelse.*
import no.nav.paw.besvarelse.Utdanning
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.Hendelse
import no.nav.paw.migrering.app.mapping.Nus
import no.nav.paw.migrering.app.mapping.toIso8601
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse as InterntHendelse

class AppTest : FreeSpec({
    "Vi sender inn start, besvarelse også stopp før vi validerer resultatet" - {
        val eventLogIterator = prepareBatches(
            periodeHendelseMeldinger = listOf(emptyList(), listOf("1" to arbeidssokerperiodeStartet, "1" to arbeidsokerperiodeStoppet)).asSequence(),
            besvarelseHendelser = listOf(listOf("123z" to besvarelse), emptyList(), emptyList()).asSequence(),
            opplysningerFraVeilarbHendelser = listOf<List<Pair<String, InterntHendelse>>>(emptyList(), listOf("dsa1" to situasjonMotattFraVeilarb), emptyList()).asSequence()
        ).iterator()
        "1. batch skal inneholde 1 hendelse basert på innsendt besvarelse" {
            eventLogIterator.hasNext() shouldBe true
            val hendelser = eventLogIterator.next()
            hendelser.size shouldBe 1
            val hendelse = hendelser.first()
            hendelse.shouldBeInstanceOf<OpplysningerOmArbeidssoekerMottatt>()
            hendelse.identitetsnummer shouldBe besvarelse.foedselsnummer
            hendelse.metadata.utfoertAv.id shouldBe "paw-migrering-veilarb-til-nytt-register"
            hendelse.metadata.utfoertAv.type shouldBe BrukerType.SYSTEM
            with(hendelse.opplysningerOmArbeidssoeker) {
                arbeidserfaring.harHattArbeid shouldBe JaNeiVetIkke.JA
                utdanning.bestaatt shouldBe JaNeiVetIkke.NEI
                utdanning.godkjent shouldBe JaNeiVetIkke.VET_IKKE
                utdanning.nus shouldBe Nus.univeristetHoeyskoleLavereNivaa
                helse.helsetilstandHindrerArbeid shouldBe JaNeiVetIkke.NEI
                jobbsituasjon.beskrivelser.size shouldBe 1
                annet.andreForholdHindrerArbeid shouldBe JaNeiVetIkke.NEI
                val beskrivelse = jobbsituasjon.beskrivelser.first()
                beskrivelse.beskrivelse shouldBe JobbsituasjonBeskrivelse.ER_PERMITTERT
                beskrivelse.detaljer[PROSENT] shouldBe "75"
                beskrivelse.detaljer[GJELDER_FRA_DATO] shouldBe besvarelse.besvarelse.dinSituasjon.tilleggsData.gjelderFraDato.toIso8601()
            }
        }
        "2. batch skal inneholde 3 hendelser" - {
            "2. batch skal være tilgjengelig" { eventLogIterator.hasNext() shouldBe true }
            val hendelser = eventLogIterator.next()
            "2.batch skal inneholde 3 hendelser" { hendelser.size shouldBe 3 }
            "1. hendelse i batchen skal være periode startet" {
                val hendelse = hendelser.first()
                hendelse.shouldBeInstanceOf<Startet>()
                hendelse.identitetsnummer shouldBe arbeidssokerperiodeStartet.foedselsnummer
                hendelse.metadata.utfoertAv.id shouldBe "paw-migrering-veilarb-til-nytt-register"
                hendelse.metadata.utfoertAv.type shouldBe BrukerType.SYSTEM
                hendelse.metadata.tidspunkt shouldBe arbeidssokerperiodeStartet.tidspunkt.truncatedTo(ChronoUnit.MILLIS)
            }
            "2. hendelse i batchen skal være periode stoppet" {
                val hendelse = hendelser[1]
                hendelse.shouldBeInstanceOf<Avsluttet>()
                hendelse.identitetsnummer shouldBe arbeidsokerperiodeStoppet.foedselsnummer
                hendelse.metadata.utfoertAv.id shouldBe "paw-migrering-veilarb-til-nytt-register"
                hendelse.metadata.utfoertAv.type shouldBe BrukerType.SYSTEM
                hendelse.metadata.tidspunkt shouldBe arbeidsokerperiodeStoppet.tidspunkt.truncatedTo(ChronoUnit.MILLIS)
            }
            "3. hendelse i batchen skal være opplysninger mottatt fra veilarb" {
                val hendelse = hendelser[2]
                hendelse.shouldBeInstanceOf<OpplysningerOmArbeidssoekerMottatt>()
                hendelse.shouldBe(situasjonMotattFraVeilarb)
            }
        }
        "Ingen flere hendelser skal være tilgjengelig" {
            eventLogIterator.hasNext() shouldBe false
        }
    }
})

val arbeidssokerperiodeStartet: ArbeidssokerperiodeHendelseMelding = ArbeidssokerperiodeHendelseMelding(
    Hendelse.STARTET,
    "12345678901",
    Instant.parse("2001-06-29T15:24:08+02:00"),
)

val arbeidsokerperiodeStoppet: ArbeidssokerperiodeHendelseMelding = ArbeidssokerperiodeHendelseMelding(
    Hendelse.STOPPET,
    "12345678901",
    Instant.parse("2001-12-29T11:24:08z")
)

val besvarelse: ArbeidssokerBesvarelseEvent = ArbeidssokerBesvarelseEvent(
    /* id = */ 1,
    /* registreringsId = */ 2,
    /* foedselsnummer = */ "12345678901",
    /* aktorId = */ "navXy141",
    /* endretTidspunkt = */ Instant.parse("2001-06-29T15:24:08+02:00"),
    /* registreringsTidspunkt = */ Instant.parse("2002-07-29T15:24:08+02:00"),
    /* opprettetAv = */ OpprettetAv.BRUKER,
    /* endretAv = */ EndretAv.VEILEDER,
    /* endret = */ true,
    /* besvarelse = */ Besvarelse(
        Utdanning(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "Mellomste Bukken Bruse",
            LocalDate.of(1912, 1, 23),
            LocalDate.of(1913, 2, 14),
            UtdanningSvar.HOYERE_UTDANNING_1_TIL_4
        ),
        UtdanningBestatt(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "Prinsessen",
            LocalDate.of(1913, 12, 1),
            LocalDate.of(1914, 11, 2),
            UtdanningBestattSvar.NEI
        ),
        UtdanningGodkjent(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "Trollet",
            LocalDate.of(1914, 12, 1),
            LocalDate.of(1915, 11, 2),
            UtdanningGodkjentSvar.VET_IKKE
        ),
        HelseHinder(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "Askeladden",
            LocalDate.of(1916, 2, 3),
            LocalDate.of(1917, 5, 4),
            HelseHinderSvar.NEI
        ),
        AndreForhold(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "Per",
            LocalDate.of(1918, 2, 3),
            LocalDate.of(1919, 5, 4),
            AndreForholdSvar.NEI
        ),
        SisteStilling(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "Pål",
            LocalDate.now(),
            LocalDate.now(),
            SisteStillingSvar.HAR_HATT_JOBB
        ),
        DinSituasjon(
            Instant.parse("2001-06-29T15:24:08+02:00"),
            "noen",
            LocalDate.now(),
            LocalDate.now(),
            DinSituasjonSvar.ER_PERMITTERT,
            DinSituasjonTilleggsData(
                null,
                null,
                null,
                LocalDate.of(1980, 9, 9),
                "75",
                null,
                null,
                null
            )
        )
    )
)

val situasjonMotattFraVeilarb = OpplysningerOmArbeidssoekerMottatt(
    hendelseId = UUID.randomUUID(),
    identitetsnummer = "12345678901",
    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = Metadata(
            tidspunkt = Instant.parse("2001-06-29T15:24:08.999+02:00"),
            utfoertAv = Bruker(
                type = BrukerType.SYSTEM,
                id = "paw-migrering-veilarb-til-nytt-register"
            ),
            kilde = "veilarbregistrering",
            aarsak = "overføring"
        ),
        utdanning = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning(
            nus = Nus.univeristetHoeyskoleLavereNivaa,
            bestaatt = JaNeiVetIkke.NEI,
            godkjent = JaNeiVetIkke.VET_IKKE
        ),
        helse = Helse(
            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
        ),
        arbeidserfaring = Arbeidserfaring(
            harHattArbeid = JaNeiVetIkke.JA
        ),
        jobbsituasjon = Jobbsituasjon(
            beskrivelser = listOf(
                JobbsituasjonMedDetaljer(
                    beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                    detaljer = mapOf(
                        PROSENT to "75",
                        GJELDER_FRA_DATO to "2020-01-01"
                    )
                )
            )
        ),
        annet = Annet(
            andreForholdHindrerArbeid = JaNeiVetIkke.NEI
        )
    )
)

