package no.nav.paw.migrering.app.mapping

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding

fun tilPeriode(bruker: Bruker, periode: ArbeidssokerperiodeHendelseMelding): Hendelse =
    when (periode.hendelse) {
        no.nav.paw.migrering.Hendelse.STARTET -> periode.toStartEvent(bruker)
        no.nav.paw.migrering.Hendelse.STOPPET -> periode.toAvsluttetEvent(bruker)
    }
