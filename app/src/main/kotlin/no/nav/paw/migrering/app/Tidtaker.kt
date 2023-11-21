package no.nav.paw.migrering.app

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

private val logger = LoggerFactory.getLogger("tidtaker")
fun <R> loggTid(id: String, funksjon: () -> R):R {
    val startet = Instant.now()
    return try {
        funksjon()
    } finally {
        val sluttet = Instant.now()
        println("$id tok ${Duration.between(startet, sluttet).abs()}")
    }
}

fun <R> (() -> R).loggTid(id: String):R = loggTid(id, this)
