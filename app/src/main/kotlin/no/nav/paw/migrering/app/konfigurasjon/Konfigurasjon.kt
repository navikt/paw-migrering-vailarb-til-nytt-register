package no.nav.paw.migrering.app.konfigurasjon

import no.nav.paw.migrering.app.lastKonfigurasjon


val kafkaKonfigurasjon: KafkaKonfigurasjon by lazy { lastKonfigurasjon("kafka_konfigurasjon.toml") }
val applikasjonKonfigurasjon: ApplikasjonKonfigurasjon by lazy { lastKonfigurasjon("applikasjon_konfigurasjon.toml") }
val databaseKonfigurasjon: DatabaseKonfigurasjon by lazy { lastKonfigurasjon("postgres.toml") }
