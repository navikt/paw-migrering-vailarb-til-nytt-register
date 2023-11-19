package no.nav.paw.migrering.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.ArbeidsoekersituasjonBeskrivelse
import no.nav.paw.besvarelse.*
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.Hendelse
import no.nav.paw.migrering.app.konfigurasjon.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class TopologyTest : StringSpec({
    val streamsBuilder = StreamsBuilder()
    streamsBuilder.addStateStore(
        KeyValueStoreBuilder(
            InMemoryKeyValueBytesStoreSupplier("db"),
            Serdes.Long(),
            TilstandSerde(),
            Time.SYSTEM
        )
    )
    "vi kan få en start event igjennom" {
        val topology = topology(
            kafkaKonfigurasjon = kafkaKonfigurasjon,
            streamBuilder = streamsBuilder,
            kafkaKeysClient = InMemKafkaKeysClient(),
            sortererKonfigurasjon = sortererKonfigurasjon
        )
        val testDriver = TopologyTestDriver(topology, kafkaKonfigurasjon.properties.toProperties())
        val eventlogTopic = testDriver.createOutputTopic(
            "hendelse",
            Serdes.Long().deserializer(),
            HendelseSerde().deserializer()
        )
        val veilarbPeriodeTopic = testDriver.createInputTopic(
            kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic,
            Serdes.String().serializer(),
            ArbeidssoekerEventSerde().serializer()
        )
        val veilarbBesvarelseTopic = testDriver.createInputTopic(
            kafkaKonfigurasjon.streamKonfigurasjon.situasjonTopic,
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
        Thread.sleep(20000)
        eventlogTopic.isEmpty shouldBe false
        val hendelse1 = eventlogTopic.readValue()
        hendelse1.shouldBeInstanceOf<Startet>()
        hendelse1.identitetsnummer shouldBe "12345678909"

        eventlogTopic.isEmpty shouldBe false
        val hendelse2 = eventlogTopic.readValue()
        hendelse2.shouldBeInstanceOf<SituasjonMottatt>()
        hendelse2.identitetsnummer shouldBe "12345678909"
        hendelse2.situasjon.arbeidsoekersituasjon.beskrivelser.size shouldBe 1
        hendelse2.situasjon.arbeidsoekersituasjon.beskrivelser.first().beskrivelse shouldBe ArbeidsoekersituasjonBeskrivelse.ER_PERMITTERT
        hendelse2.situasjon.arbeidsoekersituasjon.beskrivelser.first().detaljer[PROSENT] shouldBe "45"
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

val sortererKonfigurasjon  = HendelseSortererKonfigurasjon(
    interval = Duration.ZERO,
    forsinkelse = Duration.ZERO,
    tilstandsDbNavn = "db",
)
