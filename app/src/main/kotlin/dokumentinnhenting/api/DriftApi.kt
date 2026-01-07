package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.syfo.oppslag.BehandlerOppslagResponse
import dokumentinnhenting.integrasjoner.syfo.oppslag.FritekstRequest
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.driftApi() {
    // Utvider drift API-et til motoren
    route("/drift/api") {
        route("/syfo/behandleroppslag/search").authorizedPost<Unit, List<BehandlerOppslagResponse>, FritekstRequest>(
            AuthorizationBodyPathConfig(operasjon = Operasjon.DRIFTE, applicationsOnly = false)
        ) { _, req ->
            val behandlere = SyfoGateway().frisøkBehandlerOppslag(req.fritekst, token())

            respond(behandlere)
        }
    }
}
