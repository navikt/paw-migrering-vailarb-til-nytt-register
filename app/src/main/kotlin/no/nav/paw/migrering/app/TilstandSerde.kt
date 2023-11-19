package no.nav.paw.migrering.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class TilstandSerde : Serde<Tilstand> {
    private val objectMapper = ObjectMapper()
        .registerModules(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
            com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()
        )


    override fun serializer() = TilstandSerializer(objectMapper)
    override fun deserializer() = TilstandDeserializer(objectMapper)
}

class TilstandSerializer(private val objectMapper: ObjectMapper): Serializer<Tilstand> {
    override fun serialize(topic: String?, data: Tilstand?): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}

class TilstandDeserializer(private val objectMapper: ObjectMapper): Deserializer<Tilstand> {
    override fun deserialize(topic: String?, data: ByteArray?): Tilstand? {
        if (data == null) return null
        return objectMapper.readValue(data, Tilstand::class.java)
    }
}