package dokumentinnhenting.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Routing.actuator(prometheus: PrometheusMeterRegistry/*, stream: Stream*/) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }
        get("/live") {
            call.respond(HttpStatusCode.OK, "live")
        }
        get("/ready") {
            val status = HttpStatusCode.OK
            call.respond(status, "Ready")
            /*
            if ( stream.ready()) {
                val status = HttpStatusCode.OK
                call.respond(status, "Ready")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "Kjører ikke")
            }*/
        }
    }
}
