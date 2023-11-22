package no.nav.paw.migrering.app.db

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.serde.HendelseSerde
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.select

context(Transaction)
fun hentBatch(antall: Int): List<Pair<Long, Hendelse?>> =
    HendelserTabell
        .select { Op.TRUE }
        .orderBy(HendelserTabell.tidspunkt to SortOrder.ASC)
        .limit(antall)
        .map { resultRow ->
            resultRow[HendelserTabell.id] to HendelseSerde().deserializer().deserialize("", resultRow[HendelserTabell.hendelse])
        }
