import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.saf.*
import no.nav.aap.komponenter.httpklient.auth.token

fun NormalOpenAPIRoute.saf() {
    route("/saf").post<Unit, List<Doc>, HelseDocRequest> { _, req ->
        val token = this.token()
        respond(dokumentFilterDokumentSøk(SafClient.hentDokumenterForSak(req.saksnummer, token)))
    }

    route("/saf/{journalpostId}/{dokumentId}").get<DokumentRef,SafDocumentResponse>{ req ->
        val token = this.token()

        val gateway = SafHentDokumentGateway.withDefaultRestClient()
        val dokument = gateway.hentDokument(req.journalpostId, req.dokumentId, token)
        respond(dokument)
    }
}

data class DokumentRef(@JsonValue @PathParam("journalpostId") val journalpostId: String, @JsonValue @PathParam("dokumentId") val dokumentId: String)