package no.nav.paw.migrering.app.db

import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

fun Transaction.updateCurrentGroupId(actualGroupId: String): Boolean {
    val current = GroupIdTabell
        .selectAll()
        .map { it[GroupIdTabell.id] to it[GroupIdTabell.groupId] }
    return when {
        current.isEmpty() -> {
            GroupIdTabell.insert {
                it[groupId] = actualGroupId
            }
            false
        }
        current.map { it.second }.contains(actualGroupId) -> false
        else -> {
            GroupIdTabell.deleteAll()
            GroupIdTabell.insert {
                it[groupId] = actualGroupId
            }
            true
        }
    }
}
