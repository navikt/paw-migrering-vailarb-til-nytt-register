package no.nav.paw.migrering.app

import kotlinx.coroutines.runBlocking
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.Hendelse
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.toProperties
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Repartitioned
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse as ArbSoekerHendelse

fun main() {
    val kafkaKonfigurasjon: KafkaKonfigurasjon = lastKonfigurasjon("kafka_konfigurasjon.toml")
    val applikasjonKonfigurasjon: ApplikasjonKonfigurasjon = lastKonfigurasjon("applikasjon_konfigurasjon.toml")

    val dependencies = createDependencies(applikasjonKonfigurasjon)

    val topology = topology(
        kafkaKonfigurasjon = kafkaKonfigurasjon,
        streamBuilder = StreamsBuilder(),
        veilarbPeriodeTopic = kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic,
        veilarbBesvarelseTopic = kafkaKonfigurasjon.streamKonfigurasjon.situasjonTopic,
        hendelseTopic = kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic,
        kafkaKeysClient = dependencies.kafkaKeysClient
    )

    val streams = KafkaStreams(topology, kafkaKonfigurasjon.properties.toProperties())
    streams.start()

    Runtime.getRuntime().addShutdownHook(Thread(streams::close))
}

fun topology(
    kafkaKonfigurasjon: KafkaKonfigurasjon,
    streamBuilder: StreamsBuilder,
    veilarbPeriodeTopic: String,
    veilarbBesvarelseTopic: String,
    hendelseTopic: String,
    kafkaKeysClient: KafkaKeysClient
): Topology {
    val periodeStrøm: KStream<Long, ArbSoekerHendelse> = streamBuilder.stream(
        veilarbPeriodeTopic,
        Consumed.with(
            Serdes.String(),
            ArbeidssoekerEventSerde()
        ).withTimestampExtractor { record, _ ->
            (record.value() as? ArbeidssokerperiodeHendelseMelding)?.tidspunkt?.toEpochMilli() ?: 0L
        }
    ).map { _, melding ->
        val hendelse = when (melding.hendelse) {
            Hendelse.STARTET -> melding.toStartEvent()
            Hendelse.STOPPET -> melding.toAvsluttetEvent()
        }
        val key = runBlocking { kafkaKeysClient.getKey(melding.foedselsnummer) }
        KeyValue(key.id, hendelse)
    }.repartition(Repartitioned.with(Serdes.Long(), HendelseSerde()))

    val besvarelseStrøm: KStream<Long, ArbSoekerHendelse> = streamBuilder.stream(
        veilarbBesvarelseTopic,
        Consumed.with(
            Serdes.String(),
            kafkaKonfigurasjon.opprettSerde<ArbeidssokerBesvarelseEvent>()
        )
    ).map { _, arbeidssokerBesvarelseEvent ->
        val key = runBlocking { kafkaKeysClient.getKey(arbeidssokerBesvarelseEvent.foedselsnummer) }
        val hendelse = arbeidssokerBesvarelseEvent.tilSituasjonMottat()
        KeyValue(key.id, hendelse as ArbSoekerHendelse)
    }.repartition(Repartitioned.with(Serdes.Long(), HendelseSerde()))

    periodeStrøm
        .merge(besvarelseStrøm)
        .to(hendelseTopic, Produced.with(Serdes.Long(), HendelseSerde()))

    return streamBuilder.build()
}
