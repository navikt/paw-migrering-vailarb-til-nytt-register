package no.nav.paw.migrering.app

import kotlin.reflect.KFunction2

operator fun <T1, T2, R> ((T1, T2) -> R).invoke(t1: T1): (T2) -> R = { t2 -> this(t1, t2) }

operator fun <T1, T2, T3, R> ((T1, T2, T3) -> R).invoke(t1: T1): (T2, T3) -> R = { t2, t3 -> this(t1, t2, t3) }

inline fun <reified T1, reified T2, R> withContexts(t1: T1, t2: T2, function: KFunction2<T1, T2, R>): R = function(t1, t2)


fun test() {
    with("a") {
        with(12) {
            println("${toByte()} ${chars()}")
        }
    }
}
