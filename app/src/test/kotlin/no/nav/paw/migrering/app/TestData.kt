package no.nav.paw.migrering.app

import ArbeidssokerperiodeHendelseMelding
import java.time.Instant

data class TestData(
    val hendelser: List<TimestampOgHendelse>
)

data class TimestampOgHendelse(
    val timestamp: Instant,
    val hendelse: ArbeidssokerperiodeHendelseMelding
)
