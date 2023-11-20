package no.nav.paw.migrering.app.konfigurasjon

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

data class DatabaseKonfigurasjon(
    val host: String,
    val port: Int,
    val brukernavn: String,
    val passord: String,
    val databasenavn: String,
) {
    val url get() = "jdbc:postgresql://$host:$port/$databasenavn?user=$brukernavn&password=$passord"
}

fun DatabaseKonfigurasjon.dataSource() =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = url
        driverClassName = "org.postgresql.Driver"
        password = passord
        username = brukernavn
        isAutoCommit = false
    })
