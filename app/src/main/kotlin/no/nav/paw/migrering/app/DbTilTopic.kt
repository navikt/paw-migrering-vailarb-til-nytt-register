package no.nav.paw.migrering.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.db.hentBatch
import no.nav.paw.migrering.app.db.slett
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysResponse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.jetbrains.exposed.sql.transactions.transaction

context (PrometheusMeterRegistry)
fun hentDataFraDbOgSendTilTopic(
    topic: String,
    producer: KafkaProducer<Long, Hendelse>,
    identitetsnummerTilKafkaKey: (String) -> KafkaKeysResponse
) {
    loggTid(Operations.BATCH_SEND_TO_TOPIC) {
        val antallSlettetFraDb = transaction {
            val batch = hentBatch(2000)
            if (batch.any { (id, _) -> !slett(id) }) {
                logger.warn("Prøvde å slette data samtidig med annen node, avbryter denne batchen uten endringer!")
                rollback()
                return@transaction 0L
            } else {
                batch.mapNotNull(Pair<Long, Hendelse?>::second)
                    .map { hendelse ->
                        loggTid(Operations.LOOKUP_IN_KAFKA_KEYS) {
                            identitetsnummerTilKafkaKey(hendelse.identitetsnummer).key to hendelse
                        }
                    }
            }.map { (key, hendelse) ->
                ProducerRecord(
                    topic,
                    null,
                    hendelse.metadata.tidspunkt.toEpochMilli(),
                    key,
                    hendelse,
                    RecordHeaders()
                )
            }.also { records ->
                loggTid(Operations.BATCH_SEND_TO_TOPIC) {
                    records.map { record ->
                        producer.send(record)
                    }.let { futures ->
                        producer.flush()
                        futures.forEach { it.get() }
                    }
                }
            }.count().toLong()
        }
        antallHendelserIDB.addAndGet(-antallSlettetFraDb)
    }
}
