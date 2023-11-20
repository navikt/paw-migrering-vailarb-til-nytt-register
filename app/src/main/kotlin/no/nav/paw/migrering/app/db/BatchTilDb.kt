package no.nav.paw.migrering.app.db

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.HendelseSerde
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

fun skrivBatchTilDb(batch: List<Hendelse>) {
    transaction {
        HendelserTabell.batchInsert(data = batch, body = { hendelse ->
            this[HendelserTabell.hendelse] = HendelseSerde().serializer().serialize("", hendelse)
            this[HendelserTabell.tidspunkt] = hendelse.metadata.tidspunkt
        })
    }
}
