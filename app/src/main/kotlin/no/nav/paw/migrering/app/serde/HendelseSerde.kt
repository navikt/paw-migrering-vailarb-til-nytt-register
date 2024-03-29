package no.nav.paw.migrering.app.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class HendelseSerde : Serde<Hendelse> {
    private val objectMapper = hendelseObjectMapper()
    override fun serializer() = HendelseSerializer(objectMapper)
    override fun deserializer() = HendelseDeserializer(objectMapper)
}

class HendelseSerializer(private val objectMapper: ObjectMapper) : Serializer<Hendelse> {
    constructor() : this(hendelseObjectMapper())

    override fun serialize(topic: String?, data: Hendelse?): ByteArray {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        } ?: ByteArray(0)
    }
}

class HendelseDeserializer(private val objectMapper: ObjectMapper) : Deserializer<Hendelse> {
    constructor() : this(hendelseObjectMapper())

    override fun deserialize(topic: String?, data: ByteArray?): Hendelse? {
        if (data == null) return null
        return deserialize(objectMapper, data)
    }
}

private fun hendelseObjectMapper(): ObjectMapper = ObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerModules(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, true)
            .configure(KotlinFeature.NullToEmptyMap, true)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build(),
        JavaTimeModule()
    )

fun deserialize(objectMapper: ObjectMapper, json: ByteArray): Hendelse {
    val node = objectMapper.readTree(json)
    return when (val hendelseType = node.get("hendelseType")?.asText()) {
        startetHendelseType -> objectMapper.readValue<Startet>(node.traverse())
        avsluttetHendelseType -> objectMapper.readValue<Avsluttet>(node.traverse())
        avvistHendelseType -> objectMapper.readValue<Avvist>(node.traverse())
        avvistStoppAvPeriodeHendelseType -> objectMapper.readValue<AvvistStoppAvPeriode>(node.traverse())
        opplysningerOmArbeidssoekerHendelseType -> gammelTilNyOpplysningsKonverterer(objectMapper, node)
        else -> throw IllegalArgumentException("Ukjent hendelse type: '$hendelseType'")
    }
}

const val JA = "JA"
const val NEI = "NEI"
const val VET_IKKE = "VET_IKKE"
fun gammelTilNyOpplysningsKonverterer(objectMapper: ObjectMapper, node: JsonNode): OpplysningerOmArbeidssoekerMottatt {
    val harHattArbeid = (node.get("opplysningerOmArbeidssoeker")
        ?.get("arbeidserfaring")
        ?.get("harHattArbeid")
        ?.asText()
        ?.uppercase() ?: VET_IKKE)
    if (node.get("id") == null) {
        (node as ObjectNode).put("id", 0L)
    }
    val obj = objectMapper.readValue<OpplysningerOmArbeidssoekerMottatt>(node.traverse())
    return when (harHattArbeid) {
        JA -> obj
        NEI -> leggTilAldriHattJobb(obj)
        else -> obj
    }
}

private fun leggTilAldriHattJobb(obj: OpplysningerOmArbeidssoekerMottatt) = obj.copy(
    opplysningerOmArbeidssoeker = obj.opplysningerOmArbeidssoeker.copy(
        jobbsituasjon = obj.opplysningerOmArbeidssoeker.jobbsituasjon.copy(
            beskrivelser = obj.opplysningerOmArbeidssoeker.jobbsituasjon.beskrivelser +
                JobbsituasjonMedDetaljer(
                    beskrivelse = JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB,
                    detaljer = emptyMap()
                )
        )
    )
)
