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
            startet = (this.startet + hendelse)
        ) ?: Tilstand(
            startet = listOf(hendelse),
            avsluttet = emptyList(),
            situasjonMottatt = emptyList()
        )
        is Avsluttet -> this?.copy(
            sistEndret = Instant.now(),
            avsluttet = (this.avsluttet + hendelse)
        ) ?: Tilstand(
            startet = emptyList(),
            avsluttet = listOf(hendelse),
            situasjonMottatt = emptyList()
        )
        is SituasjonMottatt -> this?.copy(
            sistEndret = Instant.now(),
            situasjonMottatt = (this.situasjonMottatt + hendelse)
        ) ?: Tilstand(
            startet = emptyList(),
            avsluttet = emptyList(),
            situasjonMottatt = listOf(hendelse)
        )

        else -> {throw IllegalArgumentException("Ukjent hendelse: $hendelse")}
    }
}
