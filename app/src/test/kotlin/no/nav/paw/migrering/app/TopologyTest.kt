package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import Hendelse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.intern.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.besvarelse.AndreForhold
import no.nav.paw.besvarelse.AndreForholdSvar
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.Besvarelse
import no.nav.paw.besvarelse.DinSituasjon
import no.nav.paw.besvarelse.DinSituasjonSvar
import no.nav.paw.besvarelse.DinSituasjonTilleggsData
import no.nav.paw.besvarelse.EndretAv
import no.nav.paw.besvarelse.HelseHinder
import no.nav.paw.besvarelse.HelseHinderSvar
import no.nav.paw.besvarelse.OpprettetAv
import no.nav.paw.besvarelse.SisteStilling
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.besvarelse.Utdanning
import no.nav.paw.besvarelse.UtdanningBestatt
import no.nav.paw.besvarelse.UtdanningBestattSvar
import no.nav.paw.besvarelse.UtdanningGodkjent
import no.nav.paw.besvarelse.UtdanningGodkjentSvar
import no.nav.paw.besvarelse.UtdanningSvar
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.KafkaServerKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.SchemaRegistryKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.StreamKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.toProperties
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Instant
import java.time.LocalDate

class TopologyTest : StringSpec({
    "vi kan få en start event igjennom" {
        val topology = topology(
            kafkaKonfigurasjon = kafkaKonfigurasjon,
            streamBuilder = StreamsBuilder(),
            veilarbPeriodeTopic = "veilarb.periode",
            veilarbBesvarelseTopic = "veilarb.besvarelse",
            hendelseTopic = "hendelse",
            kafkaKeysClient = InMemKafkaKeysClient()
        )
        val testDriver = TopologyTestDriver(topology, kafkaKonfigurasjon.properties.toProperties())
        val eventlogTopic = testDriver.createOutputTopic(
            "hendelse",
            Serdes.Long().deserializer(),
            kafkaKonfigurasjon.opprettSerde<SpecificRecord>().deserializer()
        )
        val veilarbPeriodeTopic = testDriver.createInputTopic(
            "veilarb.periode",
            Serdes.String().serializer(),
            ArbeidssoekerEventSerde().serializer()
        )
        val veilarbBesvarelseTopic = testDriver.createInputTopic(
            "veilarb.besvarelse",
            Serdes.String().serializer(),
            kafkaKonfigurasjon.opprettSerde<ArbeidssokerBesvarelseEvent>().serializer()
        )
        veilarbPeriodeTopic.pipeInput(
            "brukes ikke",
            ArbeidssokerperiodeHendelseMelding(
                Hendelse.STARTET,
                "12345678909",
                Instant.now()
            )
        )
        veilarbBesvarelseTopic.pipeInput(
            "brukes ikke",
            ArbeidssokerBesvarelseEvent(
                12323,
                123213,
                "12345678909",
                "12345678909",
                Instant.now(),
                Instant.now(),
                OpprettetAv.BRUKER,
                EndretAv.SYSTEM,
                false,
                Besvarelse(
                    Utdanning(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        UtdanningSvar.GRUNNSKOLE
                    ),
                    UtdanningBestatt(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        UtdanningBestattSvar.JA
                    ),
                    UtdanningGodkjent(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        UtdanningGodkjentSvar.JA
                    ),
                    HelseHinder(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        HelseHinderSvar.NEI
                    ),
                    AndreForhold(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        AndreForholdSvar.NEI
                    ),
                    SisteStilling(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        SisteStillingSvar.HAR_HATT_JOBB
                    ),
                    DinSituasjon(
                        Instant.now(),
                        "12345678909",
                        LocalDate.now(),
                        LocalDate.now(),
                        DinSituasjonSvar.ER_PERMITTERT,
                        DinSituasjonTilleggsData(
                            LocalDate.now(),
                            LocalDate.now(),
                            LocalDate.now(),
                            null,
                            "45",
                            null,
                            null,
                            null
                        )
                    )
                )
            )
        )
        eventlogTopic.isEmpty shouldBe false
        val hendelse1 = eventlogTopic.readValue()
        hendelse1.shouldBeInstanceOf<Startet>()
        hendelse1.identitetsnummer shouldBe "12345678909"

        eventlogTopic.isEmpty shouldBe false
        val hendelse2 = eventlogTopic.readValue()
        hendelse2.shouldBeInstanceOf<SituasjonMottat>()
        hendelse2.identitetsnummer shouldBe "12345678909"
        hendelse2.arbeidsoekersituasjon.beskrivelser.size shouldBe 1
        hendelse2.arbeidsoekersituasjon.beskrivelser.first().beskrivelse shouldBe Beskrivelse.ER_PERMITTERT
        hendelse2.arbeidsoekersituasjon.beskrivelser.first().detaljer?.get(PROSENT) shouldBe "45"
    }
})

class InMemKafkaKeysClient : KafkaKeysClient {
    override suspend fun getKey(identitetsnummer: String): KafkaKeysResponse =
        KafkaKeysResponse(identitetsnummer.hashCode().toLong())
}

val kafkaKonfigurasjon = KafkaKonfigurasjon(
    StreamKonfigurasjon(
        "tilstandsDatabase",
        "test",
        "hendelse",
        "periode",
        "situasjon"
    ),
    KafkaServerKonfigurasjon(
        "false",
        "dummy:1234",
        null,
        null,
        null
    ),
    SchemaRegistryKonfigurasjon(
        "mock://junit-registry",
        null,
        null
    )
)
