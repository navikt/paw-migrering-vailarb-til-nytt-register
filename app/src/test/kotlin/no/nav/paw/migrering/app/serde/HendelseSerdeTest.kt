package no.nav.paw.migrering.app.serde

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse

class HendelseSerdeTest: FreeSpec({
    "Kan lese opplysninger mottatt fra veilarb" - {
        "Når person aldri har hatt jobb skal 'ALDRI_HATT_JOBB' ligge i jobbsituasjon" {
            val hendelse = HendelseSerde().deserializer().deserialize("", testJson(harHattArbeid = false))
            hendelse.shouldBeInstanceOf<OpplysningerOmArbeidssoekerMottatt>()
            val beskrivelser = hendelse.opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser
            println(beskrivelser)
            beskrivelser.size shouldBe 2
            beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR }.shouldNotBeNull()
            beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB }.shouldNotBeNull()
        }
        "Når person har hatt jobb skal ikke 'ALDRI_HATT_JOBB' ligge i jobbsituasjon" {
            val hendelse = HendelseSerde().deserializer().deserialize("", testJson(harHattArbeid = true))
            hendelse.shouldBeInstanceOf<OpplysningerOmArbeidssoekerMottatt>()
            val beskrivelser = hendelse.opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser
            println(beskrivelser)
            beskrivelser.size shouldBe 1
            beskrivelser.find { it.beskrivelse == JobbsituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR }.shouldNotBeNull()
        }
    }
})


fun testJson(harHattArbeid: Boolean) = """
{
  "hendelseId": "f49b0494-706c-436e-9f30-c45c7757838d",
  "identitetsnummer": "12345678901",
  "opplysningerOmArbeidssoeker": {
    "id": "6192f14e-6512-40b6-829e-93207f8efe42",
    "metadata": {
      "tidspunkt": 1599124355,
      "utfoertAv": {
        "type": "SLUTTBRUKER",
        "id": "12345678901"
      },
      "kilde": "veilarbregistrering",
      "aarsak": "registrering"
    },
    "utdanning": {
      "nus": "6",
      "bestaatt": "JA",
      "godkjent": "JA"
    },
    "helse": {
      "helsetilstandHindrerArbeid": "NEI"
    },
    "arbeidserfaring": {
      "harHattArbeid": "${if (harHattArbeid) "JA" else "NEI"}"
    },
    "jobbsituasjon": {
      "beskrivelser": [
        {
          "beskrivelse": "IKKE_VAERT_I_JOBB_SISTE_2_AAR",
          "detaljer": {}
        }
      ]
    },
    "annet": {
      "andreForholdHindrerArbeid": "JA"
    }
  },
  "hendelseType": "intern.v1.opplysninger_om_arbeidssoeker",
  "metadata": {
     "tidspunkt": 1599124355,
     "utfoertAv": {
       "type": "SLUTTBRUKER",
       "id": "12345678901"
     },
     "kilde": "veilarbregistrering",
     "aarsak": "registrering"
   }
}
""".toByteArray()
