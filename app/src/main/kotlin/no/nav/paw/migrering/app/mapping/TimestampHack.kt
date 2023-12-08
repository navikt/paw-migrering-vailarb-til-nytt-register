package no.nav.paw.migrering.app.mapping

import java.time.Instant

/**
 * Lite hack som brukes for å kunne forskyve timestamp slik at vi kan sikre at periode start alltid
 * kommer fra 'opplysninger om arbeidssøker mottatt' hendelse. Hacken vil forskyve timestamp fra et sekund
 * til et annet og dermed skal vi kunne sikre at hendelser ikke kommer på feil dag.
 */
fun conditionallyAddOneMilliSecond(timestamp: Instant): Instant {
    val lastDigit = timestamp.toEpochMilli() % 10
    return if (lastDigit < 9L) {
        timestamp.plusMillis(1)
    } else {
        timestamp
    }
}

fun conditionallySubtractMilliSecond(timestamp: Instant): Instant {
    val lastDigit = timestamp.toEpochMilli() % 10
    return if (lastDigit > 0L) {
        timestamp.minusMillis(1)
    } else {
        timestamp
    }
}


