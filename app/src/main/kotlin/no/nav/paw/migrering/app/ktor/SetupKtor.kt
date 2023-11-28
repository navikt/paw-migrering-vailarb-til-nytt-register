package no.nav.paw.migrering.app.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    statusConsumerRebalanceListener: StatusConsumerRebalanceListener
) = embeddedServer(Netty, port = 8080) {
    configureHealthMonitoring(
        statusConsumerRebalanceListener = statusConsumerRebalanceListener,
        prometheusMeterRegistry = prometheusMeterRegistry
    )
}

private fun Application.configureHealthMonitoring(
    statusConsumerRebalanceListener: StatusConsumerRebalanceListener,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
    }
    routing {
        get("/isAlive") {
            call.respondText("ALIVE")
        }
        get("/isReady") {
            if (statusConsumerRebalanceListener.isReady()) {
                call.respondText("READY")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "Topics are being rebalanced")
            }
        }
        get("/metrics") {
            call.respondTextWriter {
                prometheusMeterRegistry.scrape(this)
            }
        }
    }
}
