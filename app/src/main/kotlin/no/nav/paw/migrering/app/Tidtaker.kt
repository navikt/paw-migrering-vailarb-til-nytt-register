package no.nav.paw.migrering.app

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.function.Supplier

private const val MIGRERINGS_OPERASJON = "paw_arbeidssokerregisteret_migrering"
const val MIGRERINGS_HENDELSE_TIL_DB = "paw_arbeidssokerregisteret_migrering_hendelse_til_db"
const val MIGRERINGS_HENDELSER_I_DB = "paw_arbeidssokerregisteret_migrering_hendelser_i_db"

private const val OPERATION = "operation"

class Operations {
    companion object {
        const val DELETE_MESSAGE_FROM_DB = "delete_message_from_db"
        const val LOOKUP_IN_KAFKA_KEYS = "lookup_in_kafka_keys"
        const val BATCH_SEND_TO_TOPIC = "batch_send_to_topic"
        const val BATCH_READ_AND_SEND = "batch_read_and_send"
        const val BATCH_READ_FROM_DB = "batch_read_from_db"
        const val BATCH_WRITE_TO_DB = "batch_write_to_db"
    }
}

fun <R> PrometheusMeterRegistry.loggTid(operationValue: String, function: () -> R): R {
    return timer(
        MIGRERINGS_OPERASJON,
        listOf(
            Tag.of(OPERATION, operationValue)
        )
    ).record(Supplier { function() })!!
}
