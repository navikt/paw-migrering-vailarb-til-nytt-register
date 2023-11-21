package no.nav.paw.migrering.app

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.db.HendelserTabell
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun skrivTilTopic(topic: String, producer: KafkaProducer<Long, Hendelse>, kafkaKeysClient: KafkaKeysClient) {
    transaction {
        HendelserTabell
            .select { Op.TRUE }
            .orderBy(HendelserTabell.tidspunkt to SortOrder.ASC)
            .limit(2000)
            .asSequence()
            .map { resultRow ->
                resultRow[HendelserTabell.id] to HendelseSerde().deserializer().deserialize("", resultRow[HendelserTabell.hendelse])
            }.filter { (key, _) ->
                loggTid("slett") {
                    HendelserTabell.deleteWhere { id.eq(key) } == 1
                }
            }.mapNotNull(Pair<Long, Hendelse?>::second)
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
            }.also { batch ->
                loggTid("send batch til topic") {
                    batch.forEach { record ->
                        producer.send(record)
                    }
                    producer.flush()
                }
            }
    }
}
