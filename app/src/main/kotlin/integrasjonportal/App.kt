package integrasjonportal

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import integrasjonportal.routes.actuator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import integrasjonportal.auth.AZURE
import integrasjonportal.auth.authentication
import integrasjonportal.integrasjoner.behandlingsflyt.BehandlingsflytClient
import integrasjonportal.integrasjoner.behandlingsflyt.BehandlingsflytException
import integrasjonportal.routes.syfo
import io.ktor.server.plugins.calllogging.*
import io.ktor.utils.io.*

val LOGGER: Logger = LoggerFactory.getLogger("aap-integrasjonportal")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api(
    config: Config = Config(),
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val behandlingsflyt = BehandlingsflytClient(config.azureConfig, config.behandlingsflytConfig, prometheus)
    /*
    * Services
    * */
    val integrasjonportalService = IntegrasjonportalService()

    install(MicrometerMetrics) { registry = prometheus }

    authentication(config.azureConfig)

    install(CallLogging) {
        level = Level.INFO
        logger = LOGGER
        format { call ->
            """
                URL:            ${call.request.local.uri}
                Status:         ${call.response.status()}
                Method:         ${call.request.httpMethod.value}
                User-agent:     ${call.request.headers["User-Agent"]}
                CallId:         ${call.request.header("x-callId") ?: call.request.header("nav-callId")}
            """.trimIndent()
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    install(StatusPages) {
        exception<BehandlingsflytException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            LOGGER.error("Feil i behandlingsflyt: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i behandlingsflyt: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<Throwable> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause)
            LOGGER.error("Feil i tjeneste: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(text = "Feil i tjeneste: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    swaggerDoc()

    routing {
        actuator(prometheus)

        authenticate(AZURE) {
            apiRoute {
                syfo()
            }
        }
    }
}

/**
 * Triks for å få NormalOpenAPIRoute til å virke med auth
 */
@KtorDsl
fun Route.apiRoute(config: NormalOpenAPIRoute.() -> Unit) {
    NormalOpenAPIRoute(
        this,
        application.plugin(OpenAPIGen).globalModuleProvider
    ).apply(config)
}

private fun Application.swaggerDoc() {
    install(OpenAPIGen) {
        // this serves OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // this serves Swagger UI on /swagger-ui/index.html
        serveSwaggerUi = true
        info {
            title = "AAP - Integrasjonportalx"
        }
    }
}