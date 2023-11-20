package no.nav.paw.migrering.app.stresstest.validator

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.HendelseSerde
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.medKeySerde
import no.nav.paw.migrering.app.konfigurasjon.medValueSerde
import no.nav.paw.migrering.app.konfigurasjon.properties
import no.nav.paw.migrering.app.lastKonfigurasjon
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

fun main() {
    val kafkaKonfigurasjon = lastKonfigurasjon<KafkaKonfigurasjon>("kafka_konfigurasjon.toml")

    val periodeConsumer = KafkaConsumer<Long, Hendelse>(
        kafkaKonfigurasjon.properties.medKeySerde(Serdes.Long())
            .medValueSerde(HendelseSerde()) +
            ("group.id" to "stress-test-validator-v2")
    )
    periodeConsumer.subscribe(listOf("arbeidssoker-hendelseslogg-beta-v1"))
    periodeConsumer.poll(Duration.ofSeconds(1))
    periodeConsumer.seekToBeginning(emptyList())
    periodeConsumer.commitAsync()
    println("Assigments: ${periodeConsumer.assignment().size}")

    periodeConsumer.assignment()
        .forEach{
            println("Partisjon: ${it.partition()}, offset: ${periodeConsumer.position(it)}")
        }
    val teller = AtomicLong(0)
    val antallFeil = AtomicLong(0)
    val forrigeTs = AtomicReference<Instant>(null)
    generateSequence {
        val records = periodeConsumer.poll(Duration.ofSeconds(30))
        if (records.isEmpty) {
            null
        } else {
            records.map { it.value() }
        }
    }.flatten()
        .onEach { teller.incrementAndGet() }
        .forEach { hendelse ->
            val eksiterendeTs = forrigeTs.getAndSet(hendelse.metadata.tidspunkt)
            if (eksiterendeTs != null && eksiterendeTs.isAfter(hendelse.metadata.tidspunkt)) {
                antallFeil.incrementAndGet()
            }
        }
    println("Antall hendelser: ${teller.get()}")
    println("Antall feil: ${antallFeil.get()}")

}
