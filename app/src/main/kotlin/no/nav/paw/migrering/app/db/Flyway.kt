package no.nav.paw.migrering.app.db

fun flywayMigrate(dataSource: javax.sql.DataSource) {
    org.flywaydb.core.Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .load()
        .migrate()
}
