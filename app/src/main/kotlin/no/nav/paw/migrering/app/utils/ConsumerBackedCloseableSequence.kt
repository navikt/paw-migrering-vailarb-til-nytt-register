package no.nav.paw.migrering.app.utils

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

fun <K, V> consumerSequence(
    kafkaProperties: Map<String, Any>,
    pollTimeout: Duration = Duration.ofMillis(250),
    commitBeforeHasNext: Boolean = true,
    commitBeforePoll: Boolean = true
): ConsumerBackedCloseableSequence<K, V> {
    val consumer = KafkaConsumer<K, V>(kafkaProperties)
    return ConsumerBackedCloseableSequence(consumer, pollTimeout, commitBeforeHasNext, commitBeforePoll)
}

class ConsumerBackedCloseableSequence<K, V>(
    private val consumer: KafkaConsumer<K, V>,
    private val pollTimeout: Duration = Duration.ofMillis(250),
    private val commitBeforeHasNext: Boolean = true,
    private val commitBeforePoll: Boolean = true
): CloseableSequence<List<Pair<K, V>>> {
    private val isClosed = AtomicBoolean(false)
    override fun iterator(): Iterator<List<Pair<K, V>>> {
        return object : Iterator<List<Pair<K, V>>> {
            override fun hasNext(): Boolean {
                if (commitBeforeHasNext) consumer.commitSync()
                return !isClosed.get()
            }

            override fun next(): List<Pair<K, V>> {
                if (!hasNext()) throw NoSuchElementException("No more elements")
                if (commitBeforePoll) consumer.commitSync()
                val records = consumer.poll(pollTimeout)
                return records.map { it.key() to it.value() }
            }
        }
    }

    override fun close() {
        isClosed.set(true)
        consumer.close()
    }

}
