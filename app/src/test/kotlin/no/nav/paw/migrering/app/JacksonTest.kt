package no.nav.paw.migrering.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone

class JacksonTest : FreeSpec({
    "Test" {
        val mapper = ObjectMapper().findAndRegisterModules().registerKotlinModule()

        val localTime1 = LocalDateTime.of(2023, 12, 13, 11, 40, 0, 0)
        val localTime2 = LocalDateTime.of(2023, 6, 13, 11, 40, 0, 0)
        val utc1 = localTime1.toInstant(ZoneOffset.UTC)
        val utc2 = localTime2.toInstant(ZoneOffset.UTC)
        val testClass = TestClass(
            text = "$localTime1 $localTime2",
            local1 = localTime1,
            local2 = localTime2,
            zonedDateTime1 = ZonedDateTime.of(localTime1, ZoneId.of("Europe/Paris")),
            zonedDateTime2 = ZonedDateTime.of(localTime2, ZoneId.of("Europe/Paris")),
            utc1 = utc1,
            utc2 = utc2
        )
        val json = mapper.writeValueAsString(testClass)
        println(json)
        println(mapper.writeValueAsString(mapper.readValue(json, TestClass::class.java)))
        println(TimeZone.getDefault())
        println("\n\n\n\n")
        println("$testClass")
        val fromJson = mapper.readValue(mapper.writeValueAsString(testClass), TestClass2::class.java)
        println(fromJson)

        println("in\t zonedDateTime1 = ${testClass.zonedDateTime1.toInstant()}")
        println("out\t zonedDateTime1 = ${fromJson.zonedDateTime1}")

        println("in\t zonedDateTime2 = ${testClass.zonedDateTime2.toInstant()}")
        println("out\t zonedDateTime2 = ${fromJson.zonedDateTime2}")

    }
})

data class TestClass(
    val text: String,
    val local1: LocalDateTime,
    val local2: LocalDateTime,
    val zonedDateTime1: ZonedDateTime,
    val zonedDateTime2: ZonedDateTime,
    val utc1: Instant,
    val utc2: Instant
)

data class TestClass2(
    val text: String,
    val local1: LocalDateTime,
    val local2: LocalDateTime,
    val zonedDateTime1: Instant,
    val zonedDateTime2: Instant,
    val utc1: Instant,
    val utc2: Instant
)

