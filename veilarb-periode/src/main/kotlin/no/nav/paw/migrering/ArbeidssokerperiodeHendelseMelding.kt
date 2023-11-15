import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.paw.arbeidssokerregisteret.intern.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet
import java.time.Instant
import java.util.*

data class ArbeidssokerperiodeHendelseMelding(
    val hendelse: Hendelse,
    val foedselsnummer: String,
    val tidspunkt: Instant
) {
    fun toStartEvent(): Startet = Startet.newBuilder().apply {
        this.identitetsnummer = foedselsnummer
        this.metadata = Metadata(
            tidspunkt,
            Bruker(BrukerType.UDEFINERT, foedselsnummer),
            "veilarbregistrering",
            "overføring"
        )
    }.build()

    fun toStoppetEvent(): Stoppet = Stoppet.newBuilder().apply {
        this.identitetsnummer = foedselsnummer
        this.metadata = Metadata(
            tidspunkt,
            Bruker(BrukerType.UDEFINERT, foedselsnummer),
            "veilarbregistrering",
            "overføring"
        )
    }.build()
}

enum class Hendelse {
    STARTET,
    STOPPET
}
