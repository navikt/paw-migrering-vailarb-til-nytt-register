package no.nav.paw.migrering.app.mapping

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun LocalDate.toIso8601(): String = this.format(DateTimeFormatter.ISO_DATE)
