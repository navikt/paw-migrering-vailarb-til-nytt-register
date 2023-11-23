package no.nav.paw.migrering.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.app.konfigurasjon.*
import no.nav.paw.migrering.app.serde.ArbeidssoekerEventSerde
import no.nav.paw.migrering.app.serde.HendelseSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import java.io.Closeable
import java.time.Duration.*
import java.util.concurrent.atomic.AtomicBoolean

fun hendelseProducer(kafkaKonfigurasjon: KafkaKonfigurasjon) = KafkaProducer<Long, Hendelse>(
    kafkaKonfigurasjon.properties.medKeySerde(
        Serdes.Long()
    ).medValueSerde(HendelseSerde())
)

fun besvarelseConsumer(kafkaKonfigurasjon: KafkaKonfigurasjon) = KafkaConsumer<String, ArbeidssokerBesvarelseEvent>(
    kafkaKonfigurasjon.propertiesMedAvroSchemaReg.medKeySerde(
        Serdes.String()
    ).medValueSerde(SpecificAvroSerde<SpecificRecord>())
)

fun periodeConsumer(kafkaKonfigurasjon: KafkaKonfigurasjon) = KafkaConsumer<String, ArbeidssokerperiodeHendelseMelding>(
    kafkaKonfigurasjon.properties.medKeySerde(Serdes.String()).medValueSerde(ArbeidssoekerEventSerde())
)

fun <T1 : Closeable, T2 : Closeable, T3 : Closeable, R> use(t1: T1, t2: T2, t3: T3, f: (T1, T2, T3) -> R): R {
    t1.use {
        t2.use {
            t3.use {
                return f(t1, t2, t3)
            }
        }
    }
}

fun <K, V> KafkaConsumer<K, V>.subscribe(consumerStatus: ConsumerRebalanceListener, topic: String) {
    subscribe(listOf(topic), consumerStatus)
}

fun <K, V, R> KafkaConsumer<K, V>.asSequence(avslutt: AtomicBoolean, mapper: ((V) -> R)): Sequence<List<R>> {
    val dirty = AtomicBoolean(false)
    return generateSequence(0) { if (avslutt.get()) null else 1 }
        .onEach {
            if (dirty.get()) {
                commitSync()
                dirty.set(false)
            }
        }
        .map { _ -> poll(ofMillis(250)) }
        .onEach { records -> dirty.set(!records.isEmpty) }
        .map { records -> records.map { record -> record.value() } }
        .map { batch -> batch.map(mapper) }
}