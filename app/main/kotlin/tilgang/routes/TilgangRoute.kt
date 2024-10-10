package tilgang.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.request.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import tilgang.*
import tilgang.auth.ident
import tilgang.auth.roller
import tilgang.auth.token
import tilgang.metrics.httpCallCounter
import tilgang.regler.parseRoller

fun NormalOpenAPIRoute.tilgang(
    tilgangService: TilgangService,
    roles: List<Role>,
    prometheus: PrometheusMeterRegistry
) {
    route("/tilgang") {
        route("/sak") {
            post<Unit, TilgangResponse, SakTilgangRequest> { _, req ->
                prometheus.httpCallCounter("/tilgang/sak").increment()

                val callId = pipeline.context.request.header("Nav-CallId") ?: "ukjent"
                val token = token()
                val roller = parseRoller(rolesWithGroupIds = roles, roller())

                val harTilgang = tilgangService.harTilgangTilSak(ident(), req, roller, token, callId)

                respond(TilgangResponse(harTilgang))
            }
        }
        route("/behandling") {
            post<Unit, TilgangResponse, BehandlingTilgangRequest> { _, req ->
                prometheus.httpCallCounter("/tilgang/behandling").increment()

                val callId = pipeline.context.request.header("Nav-CallId") ?: "ukjent"
                val token = token()
                val roller = parseRoller(rolesWithGroupIds = roles, roller())
                
                val harTilgang = tilgangService.harTilgangTilBehandling(ident(), req, roller, token, callId)
                respond(TilgangResponse(harTilgang))
            }
        }
        route("/journalpost") {
            post<Unit, TilgangResponse, JournalpostTilgangRequest> { _, req ->
                prometheus.httpCallCounter("/tilgang/journalpost").increment()

                val callId = pipeline.context.request.header("Nav-CallId") ?: "ukjent"
                val token = token()
                val roller = parseRoller(rolesWithGroupIds = roles, roller())
                
                val harTilgang = tilgangService.harTilgangTilJournalpost(ident(), req, roller, token, callId)

                respond(TilgangResponse(harTilgang))
            }
        }
    }
}
