package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.SafClient
import dokumentinnhenting.integrasjoner.saf.SafHentDokumentGateway
import dokumentinnhenting.integrasjoner.saf.Saksnummer
import dokumentinnhenting.util.Tags
import dokumentinnhenting.util.dokument.dokumentFilterDokumentSøk
import dokumentinnhenting.util.dokument.mapTilDokumentliste
import java.io.InputStream
import no.nav.aap.komponenter.httpklient.auth.token

fun NormalOpenAPIRoute.dokumentApi() {
    route("/api/dokumenter").tag(Tags.Dokumenter) {
        route("/bruker").post<Unit, List<Journalpost>, HentDokumentoversiktBrukerRequest> { _, req ->
            val dokumenter = SafClient.hentDokumenterForBruker(req.personIdent, token())

            respond(dokumenter)
        }

        route("/sak/{saksnummer}").get<HentDokumentoversiktFagsakRequest, List<Doc>> { req ->
            val dokumenter = SafClient.hentDokumenterForSak(Saksnummer(req.saksnummer), token())
                .flatMap(::mapTilDokumentliste)

            respond(dokumenter)
        }

        route("/sak/{saksnummer}/helsedokumenter").get<HentDokumentoversiktFagsakRequest, List<Doc>> { req ->
            val dokumenter = SafClient.hentDokumenterForSak(Saksnummer(req.saksnummer), token())
                .flatMap(::mapTilDokumentliste)
                .dokumentFilterDokumentSøk()

            respond(dokumenter)
        }

        route("/{journalpostId}/{dokumentinfoId}").get<HentDokumentRequest, HentDokumentResponse> { req ->
            val gateway = SafHentDokumentGateway.withDefaultRestClient()
            val response = gateway.hentDokument(req.journalpostId, req.dokumentinfoId, token())

            respond(HentDokumentResponse(response.dokument))
        }

    }
}

data class HentDokumentoversiktBrukerRequest(
    val personIdent: String,
)

data class HentDokumentoversiktFagsakRequest(
    @PathParam(description = "Saksnummer") val saksnummer: String,
)

data class HentDokumentRequest(
    @PathParam(description = "Journalpost-ID") val journalpostId: String,
    @PathParam(description = "Dokumentinfo-ID") val dokumentinfoId: String,
)

@BinaryResponse(contentTypes = ["application/pdf"])
class HentDokumentResponse(val dokument: InputStream)
