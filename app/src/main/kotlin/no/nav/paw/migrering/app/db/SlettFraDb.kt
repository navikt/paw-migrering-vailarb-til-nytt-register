package no.nav.paw.migrering.app.db

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.migrering.app.Operations
import no.nav.paw.migrering.app.loggTid
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteWhere

context(Transaction, PrometheusMeterRegistry)
fun slett(id: Long): Boolean =
    loggTid(Operations.DELETE_MESSAGE_FROM_DB) {
        HendelserTabell.deleteWhere { HendelserTabell.id.eq(id) } == 1
    }
