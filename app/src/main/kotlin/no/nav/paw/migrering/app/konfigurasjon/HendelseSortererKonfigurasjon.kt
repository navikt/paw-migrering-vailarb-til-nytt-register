package no.nav.paw.migrering.app.konfigurasjon

import org.apache.kafka.streams.processor.PunctuationType
import java.time.Duration

data class HendelseSortererKonfigurasjon(
    val interval: Duration,
    val forsinkelse: Duration,
    val tilstandsDbNavn: String
) {
    val punctuationType: PunctuationType = PunctuationType.WALL_CLOCK_TIME
}
