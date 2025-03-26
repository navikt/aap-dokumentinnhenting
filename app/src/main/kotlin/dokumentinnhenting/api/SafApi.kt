import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.dokarkiv.DokArkivClient
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest
import dokumentinnhenting.integrasjoner.saf.*
import io.ktor.http.*
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider

/**
 * Requests blir autorisert hos SAF og er derav ikke behov for Auth plugin i dette APIet
*/
fun NormalOpenAPIRoute.safApi() {
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

    route("/saf/knyttTilAnnenSak").post<Unit, HttpStatusCode, List<KopierJournalpost>> { _, req ->
        val token = this.token()
        val gateway = DokArkivClient(OnBehalfOfTokenProvider)
        req.forEach{doc ->
            gateway.knyttJournalpostTilAnnenSak(doc.journalpostId, OpprettJournalpostRequest.Bruker(
                doc.personIdent,
                OpprettJournalpostRequest.Bruker.IdType.FNR
            ),
                token.token(),
                doc.tittel)
        }
        respond(HttpStatusCode.NoContent)
    }
}

data class DokumentRef(@JsonValue @PathParam("journalpostId") val journalpostId: String, @JsonValue @PathParam("dokumentId") val dokumentId: String)