package no.nav.paw.migrering.app.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object HendelserTabell : Table("Hendelser") {
    val id = long("id").autoIncrement()
    val tidspunkt = timestamp("tidspunkt")
    val hendelse = binary("hendelse")
    override val primaryKey = PrimaryKey(id)
}
