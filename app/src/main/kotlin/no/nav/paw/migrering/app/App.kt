package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.TimestampExtractor
import java.time.Instant

fun main() {
    val kafkaKonfigurasjon: KafkaKonfigurasjon = lastKonfigurasjon("kafka_konfigurasjon.toml")
    val specificAvroSerde = SpecificAvroSerde<SpecificRecord>()

    val steamBuilder = StreamsBuilder()

}

fun toplogy(
    streamBuilder: StreamsBuilder,
    veilarbPeriodeTopic: String,
    veilarbBesvarelseTopic: String,
    hendelseTopic: String
) {
    val periodeStrøm: KStream<String, ArbeidssokerperiodeHendelseMelding> = streamBuilder.stream(
        veilarbPeriodeTopic,
        Consumed.with(
            Serdes.String(), ArbeidssoekerEventSerde()
        ).withTimestampExtractor { record, _ ->
            (record.value() as? ArbeidssokerperiodeHendelseMelding)?.tidspunkt?.toEpochMilli() ?: 0L
        }
    )
    val besvarelseStrøm: KStream<String, ArbeidssokerBesvarelseEvent> = streamBuilder.stream(
        veilarbBesvarelseTopic,
        Consumed.with(
            Serdes.String(), SpecificAvroSerde()
        )
    )

}
