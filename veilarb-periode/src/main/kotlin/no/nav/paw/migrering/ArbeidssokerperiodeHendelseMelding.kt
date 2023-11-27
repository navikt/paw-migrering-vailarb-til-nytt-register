package no.nav.paw.migrering

import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

data class ArbeidssokerperiodeHendelseMelding(
    val hendelse: Hendelse,
    val foedselsnummer: String,
    val tidspunkt: Instant
) {
    fun toStartEvent(utfoertAv: Bruker): Startet = Startet (
        identitetsnummer = foedselsnummer,
        metadata = Metadata(
            tidspunkt = tidspunkt.truncatedTo(ChronoUnit.MILLIS),
            utfoertAv = utfoertAv,
            kilde = "veilarbregistrering",
            aarsak = "overføring"
        ),
        hendelseId = UUID.randomUUID()
    )

    fun toAvsluttetEvent(utfoertAv: Bruker): Avsluttet = Avsluttet(
        identitetsnummer = foedselsnummer,
        metadata = Metadata(
            tidspunkt = tidspunkt.truncatedTo(ChronoUnit.MILLIS),
            utfoertAv = utfoertAv,
            kilde = "veilarbregistrering",
            aarsak = "overføring"
        ),
        hendelseId = UUID.randomUUID()
    )
}

enum class Hendelse {
    STARTET,
    STOPPET
}
