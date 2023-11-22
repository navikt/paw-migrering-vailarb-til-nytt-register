package no.nav.paw.migrering.app.db

import no.nav.paw.migrering.app.loggTid
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteWhere

context(Transaction)
fun slett(id: Long): Boolean =
    loggTid("slett") {
        HendelserTabell.deleteWhere { HendelserTabell.id.eq(id) } == 1
    }
