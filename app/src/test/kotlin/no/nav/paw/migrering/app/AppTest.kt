package no.nav.paw.migrering.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
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
    val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    val periodeMeldinger: List<TimestampOgHendelse> = objectMapper.readValue(resource)

    periodeMeldinger.forEach { (timestamp, hendelse) ->
        val record = ProducerRecord(
            /* topic = */ kafkaConfig.streamKonfigurasjon.periodeTopic,
            /* partition = */ null,
            /* timestamp = */ timestamp.epochSecond,
            /* key = */ UUID.randomUUID().toString(),
            /* value = */ objectMapper.writeValueAsString(hendelse)
        )
        kafkaPeriodeProducer.send(record)
    }
}
