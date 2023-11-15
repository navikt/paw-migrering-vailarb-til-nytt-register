package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer

fun main() {
    val kafkaPeriodeProducerProperties = kafkaProducerProperties(
        producerId = "test",
        keySerializer = StringSerializer::class,
        valueSerializer = StringSerializer::class
    )
    val kafkaConfig = lastKonfigurasjon<KafkaKonfigurasjon>("kafka_konfigurasjon.toml")
    val kafkaPeriodeProducer = KafkaProducer<String, String>(kafkaPeriodeProducerProperties + kafkaConfig.properties)
    // read resource file
    val resource = TopologyTest::class.java.getResource("arbeidssokerHendelseMeldingStartet")
    val objectMapper = jacksonObjectMapper()
    val arbeidssokerHendelseMeldingStartet = objectMapper.readValue(resource, ArbeidssokerperiodeHendelseMelding::class.java)
    kafkaPeriodeProducer.send()
}
