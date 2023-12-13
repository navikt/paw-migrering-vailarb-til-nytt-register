package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

val veilarbZoneId = ZoneId.of("Europe/Paris")
fun tilPeriode(bruker: Bruker, periode: ArbeidssokerperiodeHendelseMelding): Hendelse =
    when (periode.hendelse) {
        no.nav.paw.migrering.Hendelse.STARTET -> periode.toStartEvent(bruker)
        no.nav.paw.migrering.Hendelse.STOPPET -> periode.toAvsluttetEvent(bruker)
    }

fun ArbeidssokerperiodeHendelseMelding.toStartEvent(utfoertAv: Bruker): Startet = Startet (
    identitetsnummer = foedselsnummer,
    metadata = Metadata(
        tidspunkt = conditionallySubtractMilliSecond(tidspunkt.truncatedTo(ChronoUnit.MILLIS).atZone(veilarbZoneId).toInstant()),
        utfoertAv = utfoertAv,
        kilde = "veilarbregistrering",
        aarsak = "overføring"
    ),
    hendelseId = UUID.randomUUID()
)

fun ArbeidssokerperiodeHendelseMelding.toAvsluttetEvent(utfoertAv: Bruker): Avsluttet = Avsluttet(
    identitetsnummer = foedselsnummer,
    metadata = Metadata(
        tidspunkt = tidspunkt.truncatedTo(ChronoUnit.MILLIS).atZone(veilarbZoneId).toInstant(),
        utfoertAv = utfoertAv,
        kilde = "veilarbregistrering",
        aarsak = "overføring"
    ),
    hendelseId = UUID.randomUUID()
)
