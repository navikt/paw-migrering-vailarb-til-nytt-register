package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import java.time.temporal.ChronoUnit
import java.util.*

fun tilPeriode(id: Long, bruker: Bruker, periode: ArbeidssokerperiodeHendelseMelding): Hendelse =
    when (periode.hendelse) {
        no.nav.paw.migrering.Hendelse.STARTET -> periode.toStartEvent(id, bruker)
        no.nav.paw.migrering.Hendelse.STOPPET -> periode.toAvsluttetEvent(id, bruker)
    }

fun ArbeidssokerperiodeHendelseMelding.toStartEvent(id: Long, utfoertAv: Bruker): Startet = Startet (
    identitetsnummer = foedselsnummer,
    metadata = Metadata(
        tidspunkt = conditionallySubtractMilliSecond(tidspunkt.truncatedTo(ChronoUnit.MILLIS)),
        utfoertAv = utfoertAv,
        kilde = "veilarbregistrering",
        aarsak = "overføring"
    ),
    hendelseId = UUID.randomUUID(),
    id = id
)

fun ArbeidssokerperiodeHendelseMelding.toAvsluttetEvent(id: Long, utfoertAv: Bruker): Avsluttet = Avsluttet(
    identitetsnummer = foedselsnummer,
    metadata = Metadata(
        tidspunkt = tidspunkt.truncatedTo(ChronoUnit.MILLIS),
        utfoertAv = utfoertAv,
        kilde = "veilarbregistrering",
        aarsak = "overføring"
    ),
    hendelseId = UUID.randomUUID(),
    id = id
)
