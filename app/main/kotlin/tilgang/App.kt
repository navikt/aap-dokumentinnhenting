package tilgang

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import tilgang.routes.actuator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import tilgang.auth.AZURE
import tilgang.auth.authentication
import tilgang.enhet.EnhetService
import tilgang.geo.GeoService
import tilgang.integrasjoner.behandlingsflyt.BehandlingsflytClient
import tilgang.integrasjoner.behandlingsflyt.BehandlingsflytException
import tilgang.integrasjoner.msgraph.MsGraphClient
import tilgang.integrasjoner.msgraph.MsGraphException
import tilgang.integrasjoner.nom.NOMClient
import tilgang.integrasjoner.nom.NOMException
import tilgang.integrasjoner.pdl.PdlException
import tilgang.integrasjoner.pdl.PdlGraphQLClient
import tilgang.integrasjoner.saf.SafException
import tilgang.integrasjoner.skjerming.SkjermingClient
import tilgang.integrasjoner.skjerming.SkjermingException
import tilgang.redis.Redis
import tilgang.regler.RegelService
import tilgang.routes.tilgang

val LOGGER: Logger = LoggerFactory.getLogger("aap-tilgang")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api(
    config: Config = Config(),
    redis: Redis = Redis(config.redis)
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val pdl = PdlGraphQLClient(config.azureConfig, config.pdlConfig, redis, prometheus)
    val msGraph = MsGraphClient(config.azureConfig, config.msGraphConfig, redis, prometheus)
    val behandlingsflyt = BehandlingsflytClient(config.azureConfig, config.behandlingsflytConfig, redis, prometheus)
    val saf = SafGraphqlClient(config.azureConfig, config.safConfig, redis, prometheus)
    val geoService = GeoService(msGraph)
    val enhetService = EnhetService(msGraph)
    val skjermingClient = SkjermingClient(config.azureConfig, config.skjermingConfig, redis, prometheus)
    val nomClient = NOMClient(config.azureConfig, redis, config.nomConfig, prometheus)
    val regelService = RegelService(geoService, enhetService, pdl, skjermingClient, nomClient)
    val tilgangService = TilgangService(saf, behandlingsflyt, regelService)

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
        exception<PdlException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause)
            LOGGER.error("Feil i PDL: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(text = "Feil i PDL: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
        exception<MsGraphException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            LOGGER.error("Feil i MS Graph: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i Microsoft Graph: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<BehandlingsflytException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            LOGGER.error("Feil i behandlingsflyt: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i behandlingsflyt: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<SafException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            LOGGER.error("Feil i SAF: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i SAF: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<NOMException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            LOGGER.error("Feil i NOM: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i NOM: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<SkjermingException> { call, cause ->
            LOGGER.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            LOGGER.error("Feil i skjerming: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i skjerming: ${cause.message}",
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
                tilgang(tilgangService, config.roles, prometheus)
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
            title = "AAP - Tilgang"
        }
    }
}