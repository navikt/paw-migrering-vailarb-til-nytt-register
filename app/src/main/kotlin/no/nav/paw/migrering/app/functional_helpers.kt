package no.nav.paw.migrering.app

import java.util.concurrent.atomic.AtomicLong

operator fun <T1, T2, R> ((T1, T2) -> R).invoke(t1: T1): (T2) -> R = { t2 -> this(t1, t2) }

operator fun <T1, T2, T3, R> ((T1, T2, T3) -> R).invoke(t1: T1): (T2, T3) -> R = { t2, t3 -> this(t1, t2, t3) }

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
