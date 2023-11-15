package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.*

fun main() {
    val kafkaPeriodeProducerProperties = kafkaProducerProperties(
        producerId = "test",
        keySerializer = StringSerializer::class,
        valueSerializer = StringSerializer::class
    )
    val kafkaConfig = lastKonfigurasjon<KafkaKonfigurasjon>("kafka_konfigurasjon.toml")
    val kafkaPeriodeProducer = KafkaProducer<String, String>(kafkaPeriodeProducerProperties + kafkaConfig.properties)

    val resource = TopologyTest::class.java.getResource("/arbeidssokerHendelseMeldingStartet.json")
    requireNotNull(resource) { "Finner ikke resurs" }
    val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()).registerKotlinModule()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    println(objectMapper.writeValueAsString(ArbeidssokerperiodeHendelseMelding(
        hendelse = Hendelse.STARTET,
        foedselsnummer = "12345678901",
        tidspunkt = Instant.now()
    )))
    val periodeMeldinger = objectMapper.readValue(resource, TestData::class.java)

    periodeMeldinger.hendelser.forEach {(timestamp, hendelse) ->
        val record = ProducerRecord(
            /* topic = */ kafkaConfig.streamKonfigurasjon.periodeTopic,
            /* partition = */ null,
            /* timestamp = */ timestamp.toEpochMilli(),
            /* key = */ UUID.randomUUID().toString(),
            /* value = */ objectMapper.writeValueAsString(hendelse)
        )
        kafkaPeriodeProducer.send(record)
    }
}
