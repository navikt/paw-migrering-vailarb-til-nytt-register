package no.nav.paw.migrering.app.stresstest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.Hendelse
import no.nav.paw.migrering.app.*
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.opprettSerde
import no.nav.paw.migrering.app.konfigurasjon.properties
import no.nav.paw.migrering.app.konfigurasjon.propertiesMedAvroSchemaReg
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

fun main() {
    val kafkaPeriodeProducerProperties = kafkaProducerProperties(
        producerId = "test",
        keySerializer = StringSerializer::class,
        valueSerializer = StringSerializer::class
    )
    val kafkaConfig = lastKonfigurasjon<KafkaKonfigurasjon>("kafka_konfigurasjon.toml")
    val kafkaPeriodeProducer = KafkaProducer<String, String>(kafkaPeriodeProducerProperties + kafkaConfig.properties)

    val resource = AppTest::class.java.getResource("/arbeidssokerHendelseMeldingStartet.json")
    requireNotNull(resource) { "Finner ikke resurs" }
    val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    val antallPersoner = 1_500
    val personer = hentIder(antallPersoner)
    val nåtid = LocalDateTime.now()

    personer.asSequence().map { identitetsnummer ->
        periodeHendelse(
            hendelse = Hendelse.STARTET,
            identitetsnummer = identitetsnummer,
            tidspunkt = nåtid.minus(Duration.ofDays(3650))
        )
    }.plus(personer.map { identitetsnummer ->
        periodeHendelse(
            hendelse = Hendelse.STOPPET,
            identitetsnummer = identitetsnummer,
            tidspunkt = nåtid.minus(Duration.ofDays(2650))
        )
    }).plus(personer.asSequence().map { identitetsnummer ->
        periodeHendelse(
            hendelse = Hendelse.STARTET,
            identitetsnummer = identitetsnummer,
            tidspunkt = nåtid.minus(Duration.ofDays(1650))
        )
    }).plus(personer.asSequence().map { identitetsnummer ->
        periodeHendelse(
            hendelse = Hendelse.STOPPET,
            identitetsnummer = identitetsnummer,
            tidspunkt = nåtid.minus(Duration.ofDays(650))
        )
    }).forEach { periodeHendelse ->
        val record = ProducerRecord(
            /* topic = */ kafkaConfig.klientKonfigurasjon.periodeTopic,
            /* partition = */ null,
            /* timestamp = */ Instant.now().epochSecond,
            /* key = */ periodeHendelse.foedselsnummer,
            /* value = */ objectMapper.writeValueAsString(periodeHendelse)
        )
        kafkaPeriodeProducer.send(record)
    }

    val kafkaBesvarelseProducerProperties = kafkaProducerProperties(
        producerId = "test5",
        keySerializer = StringSerializer::class,
        valueSerializer = kafkaConfig.opprettSerde<ArbeidssokerBesvarelseEvent>().serializer()::class
    )
    val besvarelseProducer = KafkaProducer<String, ArbeidssokerBesvarelseEvent>(
        kafkaBesvarelseProducerProperties + kafkaConfig.propertiesMedAvroSchemaReg
    )
    val besvarelserPerPerson = 6
    personer.asSequence().flatMap { identitetsnummer ->
        (0 until besvarelserPerPerson).asSequence().map {index ->
            besvarelse(
                identitetsnummer = identitetsnummer,
                tidspunkt = nåtid.minus(Duration.ofDays(1000).multipliedBy(6L - index))
                    .toInstant(ZoneOffset.UTC)
            )
        }
    }.forEach { besvarelse ->
        besvarelseProducer.send(
            ProducerRecord(
                /* topic = */ kafkaConfig.klientKonfigurasjon.situasjonTopic,
                /* partition = */ null,
                /* timestamp = */ besvarelse.registreringsTidspunkt.epochSecond,
                /* key = */ UUID.randomUUID().toString(),
                /* value = */ besvarelse
            )
        )
    }
    kafkaPeriodeProducer.flush()
    besvarelseProducer.flush()
    besvarelseProducer.close()
    kafkaPeriodeProducer.close()
}
