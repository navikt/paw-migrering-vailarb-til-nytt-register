package no.nav.paw.migrering.app.db

import javax.sql.DataSource


fun flywayClean(dataSource: DataSource) {
    org.flywaydb.core.Flyway.configure()
        .dataSource(dataSource)
        .cleanDisabled(false)
        .load()
        .clean()
}
fun flywayMigrate(dataSource: javax.sql.DataSource) {
    org.flywaydb.core.Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .load()
        .migrate()
}
