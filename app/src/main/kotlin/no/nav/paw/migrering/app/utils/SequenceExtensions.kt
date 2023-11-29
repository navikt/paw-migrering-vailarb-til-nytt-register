package no.nav.paw.migrering.app.utils

import java.util.concurrent.atomic.AtomicLong

fun <V> Sequence<V>.nLimitFilter(numberOfConsecutiveFalseBeforeForward: Long, predicate: (V) -> Boolean): Sequence<V> {
    val emptyCount = AtomicLong(0)
    return filter {
        if (predicate(it)) {
            emptyCount.set(0)
            true
        } else {
            emptyCount.incrementAndGet() > numberOfConsecutiveFalseBeforeForward
        }
    }
}
