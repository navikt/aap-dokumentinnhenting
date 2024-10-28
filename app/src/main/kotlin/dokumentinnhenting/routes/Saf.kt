import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.HelseDocRequest
import dokumentinnhenting.integrasjoner.saf.SafClient
import no.nav.aap.komponenter.httpklient.auth.token

fun NormalOpenAPIRoute.saf() {
    route("/saf").post<Unit, List<Doc>, HelseDocRequest> { _, req ->
        val token = this.token()
        respond(SafClient.hentDokumenterForSak(req.saksnummer, token))
    }
}
