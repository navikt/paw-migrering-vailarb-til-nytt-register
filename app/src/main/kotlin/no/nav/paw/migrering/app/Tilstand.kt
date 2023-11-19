package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import java.time.Instant

data class Tilstand(
    val sistEndret: Instant = Instant.now(),
    val startet: List<Startet>,
    val avsluttet: List<Avsluttet>,
    val situasjonMottatt: List<SituasjonMottatt>
)

fun Tilstand?.leggTilHendelse(hendelse: Hendelse): Tilstand {
    return when (hendelse) {
        is Startet -> this?.copy(
            sistEndret = Instant.now(),
            startet = startet + hendelse
        ) ?: Tilstand(
            startet = listOf(hendelse),
            avsluttet = emptyList(),
            situasjonMottatt = ArrayList()
        )
        is Avsluttet -> this?.copy(
            sistEndret = Instant.now(),
            avsluttet = avsluttet + hendelse
        ) ?: Tilstand(
            startet = ArrayList(),
            avsluttet = listOf(hendelse),
            situasjonMottatt = ArrayList()
        )
        is SituasjonMottatt -> this?.copy(
            sistEndret = Instant.now(),
            situasjonMottatt = situasjonMottatt + hendelse
        ) ?: Tilstand(
            startet = emptyList(),
            avsluttet = emptyList(),
            situasjonMottatt = listOf(hendelse)
        )

        else -> {throw IllegalArgumentException("Ukjent hendelse: $hendelse")}
    }
}
