package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import Hendelse
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
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
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Instant
import java.time.LocalDate
import java.util.*

class TopologyTest : StringSpec({
    "vi kan få en start event igjennom" {
        val topology = topology(
            registryClientUrl = "mock://$SCHEMA_REGISTRY_SCOPE",
            schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE),
            streamBuilder = StreamsBuilder(),
            veilarbPeriodeTopic = "veilarb.periode",
            veilarbBesvarelseTopic = "veilarb.besvarelse",
            hendelseTopic = "hendelse",
            kafkaKeysClient = InMemKafkaKeysClient()
        )
        val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
        val eventlogTopic = testDriver.createOutputTopic(
            "hendelse",
            Serdes.Long().deserializer(),
            opprettSerde<SpecificRecord>().deserializer()
        )
        val veilarbPeriodeTopic = testDriver.createInputTopic(
            "veilarb.periode",
            Serdes.String().serializer(),
            ArbeidssoekerEventSerde().serializer()
        )
        val veilarbBesvarelseTopic = testDriver.createInputTopic(
            "veilarb.besvarelse",
            Serdes.String().serializer(),
            opprettSerde<ArbeidssokerBesvarelseEvent>().serializer()
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

fun <T : SpecificRecord> opprettSerde(): Serde<T> {
    val schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)
    val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
    serde.configure(
        mapOf(
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
        ),
        false
    )
    return serde
}

class InMemKafkaKeysClient : KafkaKeysClient {
    override suspend fun getKey(identitetsnummer: String): KafkaKeysResponse {
        return KafkaKeysResponse(identitetsnummer.hashCode().toLong())
    }
}

const val SCHEMA_REGISTRY_SCOPE = "juni-registry"
val kafkaStreamProperties = Properties().apply {
    this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
    this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
    this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
    this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
    this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
    this[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
}
