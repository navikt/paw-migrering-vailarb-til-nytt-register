package no.nav.paw.migrering.app

operator fun <T1, T2, R> ((T1, T2) -> R).invoke(t1: T1): (T2) -> R = { t2 -> this(t1, t2) }
operator fun <T1, T2, T3, R> ((T1, T2, T3) -> R).invoke(t1: T1): (T2, T3) -> R = { t2, t3 -> this(t1, t2, t3) }
