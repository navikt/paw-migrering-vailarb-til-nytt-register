package no.nav.paw.migrering.app.utils

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

fun <V> Sequence<V>.nLimitFilter(
    numberOfConsecutiveFalseBeforeForward: Long,
    numberOfConsecutiveTrueBeforeForwardAfterFirstTrigger: Long = numberOfConsecutiveFalseBeforeForward,
    predicate: (V) -> Boolean
): Sequence<V> {
    val hitCount = AtomicLong(0)
    val hasBeenHit = AtomicBoolean(false)
    return filter {
        if (predicate(it)) {
            hitCount.set(0)
            true
        } else {
            val limit = if (hasBeenHit.get()) numberOfConsecutiveTrueBeforeForwardAfterFirstTrigger
            else numberOfConsecutiveFalseBeforeForward
            val limitHit = hitCount.incrementAndGet() > limit
            if (limitHit) {
                hasBeenHit.set(true)
            }
            limitHit
        }
    }
}
