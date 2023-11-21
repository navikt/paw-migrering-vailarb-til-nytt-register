package no.nav.paw.migrering.app.konfigurasjon

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsConfig
import java.util.*
import kotlin.reflect.KClass

data class KafkaKonfigurasjon(
    val topics: TopicKonfigurasjon,
    val serverKonfigurasjon: KafkaServerKonfigurasjon,
    val schemaRegistryKonfigurasjon: SchemaRegistryKonfigurasjon
)

fun <T : SpecificRecord> KafkaKonfigurasjon.opprettSerde() = SpecificAvroSerde<T>().apply {
    configure(
        mapOf(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryKonfigurasjon.url,
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true"
        ),
        false
    )
}

fun Map<String, Any?>.toProperties(): Properties {
    val properties = Properties()
    properties.putAll(this)
    return properties
}

val KafkaKonfigurasjon.properties
    get(): Map<String, Any?> = mapOf(
        ConsumerConfig.GROUP_ID_CONFIG to "migrering_v1",
        ProducerConfig.CLIENT_ID_CONFIG to "migrering_v1",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 2000,
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to serverKonfigurasjon.kafkaBrokers,
    ) + if (serverKonfigurasjon.autentisering.equals("SSL", true)) {
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to serverKonfigurasjon.keystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to serverKonfigurasjon.credstorePassword,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to serverKonfigurasjon.truststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to serverKonfigurasjon.credstorePassword
        )
    } else emptyMap()

val KafkaKonfigurasjon.propertiesMedAvroSchemaReg
    get() = properties +
        mapOf(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryKonfigurasjon.url,
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to schemaRegistryKonfigurasjon.autoRegistrerSchema,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java.name
        )

fun Map<String, Any?>.medKeySerde(serde: Serde<*>) = this + mapOf(
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to serde.serializer()::class.java.name,
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to serde.deserializer()::class.java.name
)

fun Map<String, Any?>.medValueSerde(serde: Serde<*>) = this + mapOf(
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to serde.serializer()::class.java.name,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to serde.deserializer()::class.java.name
)

