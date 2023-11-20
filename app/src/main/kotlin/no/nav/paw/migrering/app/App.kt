package no.nav.paw.migrering.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.Hendelse
import no.nav.paw.migrering.app.db.HendelserTabell
import no.nav.paw.migrering.app.db.flywayMigrate
import no.nav.paw.migrering.app.konfigurasjon.*
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.sequences.Sequence
import kotlin.system.exitProcess
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse as InternHendelse

fun main() {
    val logger = LoggerFactory.getLogger("migrering")
    val kafkaKonfigurasjon: KafkaKonfigurasjon = lastKonfigurasjon("kafka_konfigurasjon.toml")
    val applikasjonKonfigurasjon: ApplikasjonKonfigurasjon = lastKonfigurasjon("applikasjon_konfigurasjon.toml")
    val databaseKonfigurasjon: DatabaseKonfigurasjon = lastKonfigurasjon("postgres.toml")
    val dataSource = databaseKonfigurasjon.dataSource()
    flywayMigrate(dataSource)
    Database.connect(dataSource)
    val dependencies = createDependencies(applikasjonKonfigurasjon)

    val periodeConsumer = KafkaConsumer<String, ArbeidssokerperiodeHendelseMelding>(kafkaKonfigurasjon.properties.medKeySerde(Serdes.String()).medValueSerde(ArbeidssoekerEventSerde()))
    val besvarelseConsumer = KafkaConsumer<String, ArbeidssokerBesvarelseEvent>(kafkaKonfigurasjon.propertiesMedAvroSchemaReg.medKeySerde(Serdes.String()).medValueSerde(SpecificAvroSerde<SpecificRecord>()))
    val hendelseProducer = KafkaProducer<Long, InternHendelse>(kafkaKonfigurasjon.properties.medKeySerde(Serdes.Long()).medValueSerde(HendelseSerde()))

    periodeConsumer.subscribe(listOf(kafkaKonfigurasjon.topics.periodeTopic))
    besvarelseConsumer.subscribe(listOf(kafkaKonfigurasjon.topics.situasjonTopic))

    val avslutt = AtomicBoolean(false)

    runAsync {
        val periodeSekvens: Sequence<List<InternHendelse>> = periodeConsumer.asSequence(avslutt, ::tilPeriode)
        val besvarelseSekvens: Sequence<List<InternHendelse>> = besvarelseConsumer.asSequence(avslutt) { it.tilSituasjonMottat() }

        periodeSekvens
            .zip(besvarelseSekvens)
            .map { it.first + it.second }
            .forEach { hendelser ->
                println("Hendelser: ${hendelser.size}")
                if (hendelser.isEmpty()) {
                    skrivTilTopic(
                        kafkaKonfigurasjon.topics.eventlogTopic,
                        hendelseProducer,
                        dependencies.kafkaKeysClient
                    )
                } else {
                    skrivBatch(hendelser)
                }
            }
        periodeConsumer.commitSync()
        besvarelseConsumer.commitSync()
    }.handle { _, t ->
        if (t != null) {
            logger.error("Feil ved migrering", t)
            exitProcess(1)
        } else {
            logger.info("Migrering avsluttes")
        }
    }.join()
}

val batchId = AtomicInteger(0)
fun <K, V, R> KafkaConsumer<K, V>.asSequence(avslutt: AtomicBoolean, mapper: ((V) -> R)): Sequence<List<R>> {
    val id = batchId.incrementAndGet()
    return generateSequence {
        commitSync()
        if (avslutt.get()) null else poll(Duration.ofMillis(250))
    }.map { it.map { record -> record.value() } }
        .map { batch -> batch.map(mapper) }
        .onEach { println("sekvens $id forwarder ${it.size} hendelser") }
}

tailrec fun skrivTilTopic(topic: String, producer: KafkaProducer<Long, InternHendelse>, kafkaKeysClient: KafkaKeysClient) {
    val antall = transaction {
        HendelserTabell
            .select { Op.TRUE }
            .orderBy(HendelserTabell.tidspunkt to SortOrder.ASC)
            .limit(200)
            .asSequence()
            .map { resultRow ->
                resultRow[HendelserTabell.id] to HendelseSerde().deserializer().deserialize("", resultRow[HendelserTabell.hendelse])
            }.onEach { (key, _) ->
                HendelserTabell.deleteWhere { id.eq(key) }
            }.mapNotNull(Pair<Long, InternHendelse?>::second)
            .map { hendelse ->
                runBlocking { kafkaKeysClient.getKey(hendelse.identitetsnummer) to hendelse }
            }.map { (key, hendelse) ->
                ProducerRecord(
                    topic,
                    null,
                    hendelse.metadata.tidspunkt.toEpochMilli(),
                    key.id,
                    hendelse,
                    RecordHeaders()
                )
            }
            .toList().let { batch ->
                batch.forEach { producer.send(it) }
                producer.flush()
                batch.size
            }
    }
    if (antall > 0) {
        skrivTilTopic(topic, producer, kafkaKeysClient)
    } else {
        return
    }
}


fun skrivBatch(batch: List<InternHendelse>) {
    transaction {
        val antall = HendelserTabell.batchInsert(data = batch, body = { hendelse ->
            this[HendelserTabell.hendelse] = HendelseSerde().serializer().serialize("", hendelse)
            this[HendelserTabell.tidspunkt] = hendelse.metadata.tidspunkt
        }).size
        println("Batch inneholder ${batch.size} hendelser, skrevet $antall hendelser til database")
    }
}

fun tilPeriode(periode: ArbeidssokerperiodeHendelseMelding): InternHendelse =
    when (periode.hendelse) {
        Hendelse.STARTET -> periode.toStartEvent()
        Hendelse.STOPPET -> periode.toAvsluttetEvent()
    }
