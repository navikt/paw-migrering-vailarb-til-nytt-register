package no.nav.paw.migrering.app.db

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.MIGRERINGS_HENDELSE_TIL_DB
import no.nav.paw.migrering.app.Operations
import no.nav.paw.migrering.app.antallHendelserIDB
import no.nav.paw.migrering.app.loggTid
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

context (PrometheusMeterRegistry)
fun skrivBatchTilDb(serializer: (Hendelse) -> ByteArray, batch: List<Hendelse>) {
    loggTid(Operations.BATCH_WRITE_TO_DB) {
        transaction {
            HendelserTabell.batchInsert(data = batch, body = { hendelse ->
                this[HendelserTabell.hendelse] = serializer(hendelse)
                this[HendelserTabell.tidspunkt] = hendelse.metadata.tidspunkt
            })
        }
        val batchSize = batch.size
        counter(MIGRERINGS_HENDELSE_TIL_DB).increment(batchSize.toDouble())
        antallHendelserIDB.addAndGet(batchSize.toLong())
    }
}
