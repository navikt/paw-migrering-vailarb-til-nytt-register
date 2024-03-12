package no.nav.paw.migrering.app.db

import no.nav.paw.migrering.app.konfigurasjon.kafkaKonfigurasjon
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object HendelserTabell : Table("Hendelser") {
    val id = long("id").autoIncrement()
    val tidspunkt = timestamp("tidspunkt")
    val hendelse = binary("hendelse")
    val groupId = varchar("group_id", 255).default(kafkaKonfigurasjon.klientKonfigurasjon.konsumerGruppeId)
    override val primaryKey = PrimaryKey(id)
}
