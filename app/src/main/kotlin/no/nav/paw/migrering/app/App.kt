package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import Hendelse
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanning
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream

fun main() {
    val kafkaKonfigurasjon: KafkaKonfigurasjon = lastKonfigurasjon("kafka_konfigurasjon.toml")
    val specificAvroSerde = SpecificAvroSerde<SpecificRecord>()

    val steamBuilder = StreamsBuilder()
}

fun toplogy(
    streamBuilder: StreamsBuilder,
    veilarbPeriodeTopic: String,
    veilarbBesvarelseTopic: String,
    hendelseTopic: String,
    kafkaKeysClient: KafkaKeysClient,
) {
    val periodeStrøm: KStream<Long, SpecificRecord> = streamBuilder.stream(
        veilarbPeriodeTopic,
        Consumed.with(
            Serdes.String(),
            ArbeidssoekerEventSerde(),
        ).withTimestampExtractor { record, _ ->
            (record.value() as? ArbeidssokerperiodeHendelseMelding)?.tidspunkt?.toEpochMilli() ?: 0L
        },
    ).map { _, melding ->
        val hendelse = when (melding.hendelse) {
            Hendelse.STARTET -> melding.toStartEvent()
            Hendelse.STOPPET -> melding.toStoppetEvent()
        }
        val key = runBlocking { kafkaKeysClient.getKey(melding.foedselsnummer) }
        KeyValue(key.id, hendelse)
    }
    /**
     * All-args constructor.
     * @param identitetsnummer The new value for identitetsnummer
     * @param metadata The new value for metadata
     * @param utdanning The new value for utdanning
     * @param helse The new value for helse
     * @param arbeidserfaring The new value for arbeidserfaring
     * @param arbeidsoekersituasjon The new value for arbeidsoekersituasjon
     */

    val besvarelseStrøm: KStream<Long, SpecificRecord> = streamBuilder.stream(
        veilarbBesvarelseTopic,
        Consumed.with(
            Serdes.String(),
            SpecificAvroSerde<ArbeidssokerBesvarelseEvent>(),
        ),
    ).map { _, arbeidssokerBesvarelseEvent ->
        val key = runBlocking { kafkaKeysClient.getKey(arbeidssokerBesvarelseEvent.foedselsnummer) }
        val hendelse = SituasjonMottat(
            arbeidssokerBesvarelseEvent.foedselsnummer,
            no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata(
                arbeidssokerBesvarelseEvent.registreringsTidspunkt,
                Bruker(
                    BrukerType.SLUTTBRUKER,
                    arbeidssokerBesvarelseEvent.foedselsnummer,
                ),
                "veilarbregistrering",
                "overføring",
            ),
            Utdanning(
                arbeidssokerBesvarelseEvent.besvarelse.utdanning
            )
        )
        KeyValue(key.id, arbeidssokerBesvarelseEvent)
    }
}
