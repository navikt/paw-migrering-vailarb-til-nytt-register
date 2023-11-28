package no.nav.paw.migrering.app.kafka

import no.nav.paw.migrering.app.logger
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import java.util.concurrent.atomic.AtomicInteger

class StatusConsumerRebalanceListener(vararg topics: String): ConsumerRebalanceListener {
    private val klar = 1
    private val init = 0
    private val stoppet = 2
    private val map: Map<String, AtomicInteger> = topics.associateWith { AtomicInteger(init) }
    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        partitions
            ?.map { it.topic() }
            ?.distinct()
            ?.map { topic -> topic to map[topic]?.getAndSet(stoppet) }
            ?.forEach { (topic, forrigeStatus) ->
                when (forrigeStatus) {
                    init -> logger.info("Abonnement på topic $topic er ikke klart for oppstart")
                    klar -> logger.info("Abonnement på topic $topic er midlertidig stoppet")
                    stoppet -> logger.info("Abonnement på topic $topic er uendret")
                }
            }
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        partitions
            ?.map { it.topic() }
            ?.distinct()
            ?.map { topic -> topic to map[topic]?.getAndSet(klar) }
            ?.forEach { (topic, forrigeStatus) ->
                when (forrigeStatus) {
                    init -> logger.info("Abonnement på topic $topic er startet")
                    stoppet -> logger.info("Abonnement på topic $topic er gjenopptatt")
                    klar -> logger.info("Abonnement på topic $topic er uendret")
                }
            }
    }

    fun isReady(topics: Collection<String>): Boolean {
        return topics.all { map[it]?.get() == klar }
    }

    fun isReady(): Boolean {
        return map.all { it.value.get() == klar }
    }

}
