package no.nav.paw.migrering.app.utils

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

fun <K, V> consumerSequence(
    consumerProperties: Map<String, Any?>,
    pollTimeout: Duration = Duration.ofMillis(250),
    commitBeforeHasNext: Boolean = true,
    commitBeforePoll: Boolean = true,
    subscribeTo: List<String>,
    rebalanceListener: StatusConsumerRebalanceListener? = null
): ConsumerBackedCloseableSequence<K, V> {
    val consumer = KafkaConsumer<K, V>(consumerProperties)
    if (rebalanceListener != null) {
        consumer.subscribe(subscribeTo, rebalanceListener)
    } else {
        consumer.subscribe(subscribeTo)
    }
    return ConsumerBackedCloseableSequence(consumer, pollTimeout, commitBeforeHasNext, commitBeforePoll)
}

class ConsumerBackedCloseableSequence<K, V>(
    private val consumer: KafkaConsumer<K, V>,
    private val pollTimeout: Duration = Duration.ofMillis(250),
    private val commitBeforeHasNext: Boolean = true,
    private val commitBeforePoll: Boolean = true
) : CloseableSequence<List<Pair<K, V>>> {
    private val logger = LoggerFactory.getLogger(this::class.java)
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
    val metricsBinder = KafkaClientMetrics(consumer)

    override fun close() {
        isClosed.set(true)
        consumer.close()
        metricsBinder.close()
    }

    override fun closeJustLogOnError() {
        try {
            close()
        } catch (e: Exception) {
            logger.warn("Error closing consumer", e)
        }
    }

}
