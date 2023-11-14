package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import Hendelse
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanning
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanningsnivaa
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.UtdanningSvar
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
                when (arbeidssokerBesvarelseEvent.besvarelse.utdanning.verdi) {
                    UtdanningSvar.INGEN_UTDANNING -> Utdanningsnivaa.INGEN_UTDANNING
                    UtdanningSvar.GRUNNSKOLE -> Utdanningsnivaa.GRUNNSKOLE
                    UtdanningSvar.VIDEREGAENDE_GRUNNUTDANNING -> Utdanningsnivaa.VIDEREGAENDE_GRUNNUTDANNING
                    UtdanningSvar.VIDEREGAENDE_FAGBREV_SVENNEBREV -> Utdanningsnivaa.VIDEREGAENDE_FAGBREV_SVENNEBREV
                    UtdanningSvar.HOYERE_UTDANNING_1_TIL_4 -> Utdanningsnivaa.HOYERE_UTDANNING_1_TIL_4
                    UtdanningSvar.HOYERE_UTDANNING_5_ELLER_MER -> Utdanningsnivaa.HOYERE_UTDANNING_5_ELLER_MER
                    UtdanningSvar.INGEN_SVAR -> Utdanningsnivaa.UDEFINERT
                    null -> Utdanningsnivaa.UDEFINERT
                }
                when (arbeidssokerBesvarelseEvent.besvarelse.utd) {
                }
            )
        )
        KeyValue(key.id, arbeidssokerBesvarelseEvent)
    }
}

fun jaNeiVetIkke() {
    
}