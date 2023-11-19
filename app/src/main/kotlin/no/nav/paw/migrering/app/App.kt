package no.nav.paw.migrering.app

import kotlinx.coroutines.runBlocking
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.Hendelse
import no.nav.paw.migrering.app.konfigurasjon.ApplikasjonKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.HendelseSortererKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.toProperties
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse as ArbSoekerHendelse

fun main() {
    val kafkaKonfigurasjon: KafkaKonfigurasjon = lastKonfigurasjon("kafka_konfigurasjon.toml")
    val applikasjonKonfigurasjon: ApplikasjonKonfigurasjon = lastKonfigurasjon("applikasjon_konfigurasjon.toml")

    val dependencies = createDependencies(applikasjonKonfigurasjon)
    val streamBuilder = StreamsBuilder()
    val sortererKonfigurasjon: HendelseSortererKonfigurasjon = lastKonfigurasjon("hendelse_sorterings_konfigurasjon.toml")
    streamBuilder.addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(sortererKonfigurasjon.tilstandsDbNavn, false),
            Serdes.Long(),
            TilstandSerde(),
            Time.SYSTEM
        )
    )
    val topology = topology(
        kafkaKonfigurasjon = kafkaKonfigurasjon,
        streamBuilder = streamBuilder,
        kafkaKeysClient = dependencies.kafkaKeysClient,
        sortererKonfigurasjon = sortererKonfigurasjon
    )

    val streams = KafkaStreams(topology, kafkaKonfigurasjon.properties.toProperties())
    streams.start()

    Runtime.getRuntime().addShutdownHook(Thread(streams::close))
}

fun topology(
    kafkaKonfigurasjon: KafkaKonfigurasjon,
    streamBuilder: StreamsBuilder,
    kafkaKeysClient: KafkaKeysClient,
    sortererKonfigurasjon: HendelseSortererKonfigurasjon
): Topology {
    val veilarbPeriodeTopic = kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic
    val veilarbBesvarelseTopic = kafkaKonfigurasjon.streamKonfigurasjon.situasjonTopic
    val hendelseTopic = kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic
    val periodeStrøm: KStream<Long, ArbSoekerHendelse> = streamBuilder.stream(
        veilarbPeriodeTopic,
        Consumed.with(
            Serdes.String(),
            ArbeidssoekerEventSerde()
        )
    ).map { _, melding ->
        val hendelse = when (melding.hendelse) {
            Hendelse.STARTET -> melding.toStartEvent()
            Hendelse.STOPPET -> melding.toAvsluttetEvent()
        }
        val key = runBlocking { kafkaKeysClient.getKey(melding.foedselsnummer) }
        KeyValue(key.id, hendelse)
    }

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
    }

    periodeStrøm
        .merge(besvarelseStrøm)
        .repartition(Repartitioned.with(Serdes.Long(), HendelseSerde()))
        .sorter(sortererKonfigurasjon)
        .to(hendelseTopic, Produced.with(Serdes.Long(), HendelseSerde()))

    return streamBuilder.build()
}
