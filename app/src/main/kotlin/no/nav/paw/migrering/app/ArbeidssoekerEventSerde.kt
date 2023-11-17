package no.nav.paw.migrering.app

import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class ArbeidssoekerEventSerde : Serde<ArbeidssokerperiodeHendelseMelding> {
    private val objectMapper = ObjectMapper()
        .registerModules(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
            com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()
        )

    override fun serializer() = ArbeidssokerperiodeHendelseMeldingSerializer(objectMapper)
    override fun deserializer() = ArbeidssokerperiodeHendelseMeldingDeserializer(objectMapper)
}

class ArbeidssokerperiodeHendelseMeldingSerializer(private val objectMapper: ObjectMapper) :
    Serializer<ArbeidssokerperiodeHendelseMelding> {
    override fun serialize(topic: String?, data: ArbeidssokerperiodeHendelseMelding?): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}

class ArbeidssokerperiodeHendelseMeldingDeserializer(private val objectMapper: ObjectMapper) :
    Deserializer<ArbeidssokerperiodeHendelseMelding> {
    override fun deserialize(topic: String?, data: ByteArray?): ArbeidssokerperiodeHendelseMelding? {
        if (data == null) return null
        return objectMapper.readValue(data, ArbeidssokerperiodeHendelseMelding::class.java)
    }
}
