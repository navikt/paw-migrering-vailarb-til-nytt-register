package no.nav.paw.migrering.app.db

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

fun skrivBatchTilDb(serializer: (Hendelse) -> ByteArray, batch: List<Hendelse>) {
    transaction {
        HendelserTabell.batchInsert(data = batch, body = { hendelse ->
            this[HendelserTabell.hendelse] = serializer(hendelse)
            this[HendelserTabell.tidspunkt] = hendelse.metadata.tidspunkt
        })
    }
}
