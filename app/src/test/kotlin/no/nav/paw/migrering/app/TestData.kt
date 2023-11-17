package no.nav.paw.migrering.app

import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import java.time.Instant

data class TimestampOgHendelse(
    val timestamp: Instant,
    val hendelse: ArbeidssokerperiodeHendelseMelding
)
