package no.nav.paw.migrering.app.db

import org.jetbrains.exposed.sql.Table

object GroupIdTabell : Table("Group_Id") {
    val id = long("id").autoIncrement()
    val groupId = varchar("group_id", 255)
    override val primaryKey = PrimaryKey(HendelserTabell.id)
}
