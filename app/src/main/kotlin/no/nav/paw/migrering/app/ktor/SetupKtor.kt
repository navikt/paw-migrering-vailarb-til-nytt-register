package no.nav.paw.migrering.app.ktor

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    statusConsumerRebalanceListener: StatusConsumerRebalanceListener,
    kafkaClientMetrics: List<KafkaClientMetrics>
) = embeddedServer(Netty, port = 8080) {
    configureHealthMonitoring(
        statusConsumerRebalanceListener = statusConsumerRebalanceListener,
        prometheusMeterRegistry = prometheusMeterRegistry,
        kafkaClientMetrics = kafkaClientMetrics
    )
}

private fun Application.configureHealthMonitoring(
    statusConsumerRebalanceListener: StatusConsumerRebalanceListener,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    kafkaClientMetrics: List<KafkaClientMetrics>
) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        ) + kafkaClientMetrics
    }
    routing {
        get("/isAlive") {
            call.respondText("ALIVE")
        }
        get("/isReady") {
            if (statusConsumerRebalanceListener.hasBeenInitialized()) {
                call.respondText("READY")
            } else {
                //Quickfix for nais deploy problemer
                call.respondText("READY")
                //call.respond(HttpStatusCode.ServiceUnavailable, "Listener is waiting for initial poll")
            }
        }
        get("/metrics") {
            call.respondTextWriter {
                prometheusMeterRegistry.scrape(this)
            }
        }
    }
}
