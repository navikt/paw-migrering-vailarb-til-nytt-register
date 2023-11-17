package no.nav.paw.migrering

import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import java.time.Instant
import java.util.*

data class ArbeidssokerperiodeHendelseMelding(
    val hendelse: Hendelse,
    val foedselsnummer: String,
    val tidspunkt: Instant
) {
    fun toStartEvent(): Startet = Startet (
        identitetsnummer = foedselsnummer,
        metadata = Metadata(
            tidspunkt = tidspunkt,
            utfoertAv = Bruker(BrukerType.UDEFINERT, foedselsnummer),
            kilde = "veilarbregistrering",
            aarsak = "overføring"
        ),
        hendelseId = UUID.randomUUID()
    )

    fun toAvsluttetEvent(): Avsluttet = Avsluttet(
        identitetsnummer = foedselsnummer,
        metadata = Metadata(
            tidspunkt = tidspunkt,
            utfoertAv = Bruker(BrukerType.UDEFINERT, foedselsnummer),
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
