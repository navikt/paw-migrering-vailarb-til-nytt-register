package no.nav.paw.migrering

import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class ArbeidssokerperiodeHendelseMelding(
    val hendelse: Hendelse,
    val foedselsnummer: String,
    val tidspunkt: LocalDateTime
)

enum class Hendelse {
    STARTET,
    STOPPET
}
