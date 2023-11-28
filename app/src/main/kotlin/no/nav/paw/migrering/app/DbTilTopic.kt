package no.nav.paw.migrering.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.db.hentBatch
import no.nav.paw.migrering.app.db.slett
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.jetbrains.exposed.sql.transactions.transaction

context (PrometheusMeterRegistry)
fun skrivTilTopic(topic: String, producer: KafkaProducer<Long, Hendelse>, kafkaKeysClient: KafkaKeysClient) {
    loggTid(Operations.BATCH_SEND_TO_TOPIC) {
        transaction {
            val batch = hentBatch(2000)
            if (batch.any { (id, _) -> !slett(id) }) {
                logger.warn("Prøvde å slette data samtidig med annen node, avbryter denne batchen uten endringer!")
                rollback()
                return@transaction
            } else {
                batch.mapNotNull(Pair<Long, Hendelse?>::second)
                    .map { hendelse ->
                        loggTid(Operations.LOOKUP_IN_KAFKA_KEYS) {
                            runBlocking { kafkaKeysClient.getKey(hendelse.identitetsnummer) to hendelse }
                        }
                    }.map { (key, hendelse) ->
                        ProducerRecord(
                            topic,
                            null,
                            hendelse.metadata.tidspunkt.toEpochMilli(),
                            key.id,
                            hendelse,
                            RecordHeaders()
                        )
                    }.also { batch ->
                        loggTid(Operations.BATCH_SEND_TO_TOPIC) {
                            batch.map { record ->
                                producer.send(record)
                            }.let { futures ->
                                producer.flush()
                                futures.forEach { it.get() }
                            }
                        }
                    }
            }
        }
    }
}
