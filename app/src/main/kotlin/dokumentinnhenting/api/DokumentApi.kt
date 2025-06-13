package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import dokumentinnhenting.integrasjoner.dokarkiv.DokArkivClient
import dokumentinnhenting.integrasjoner.dokarkiv.KnyttTilAnnenSakRequest
import dokumentinnhenting.integrasjoner.dokarkiv.KnyttTilAnnenSakResponse
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.SafClient
import dokumentinnhenting.integrasjoner.saf.SafHentDokumentGateway
import dokumentinnhenting.integrasjoner.saf.Saksnummer
import dokumentinnhenting.util.Tags
import dokumentinnhenting.util.dokument.dokumentFilterDokumentSøk
import dokumentinnhenting.util.dokument.mapTilDokumentliste
import io.ktor.http.HttpStatusCode
import java.io.InputStream
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider

fun NormalOpenAPIRoute.dokumentApi() {
    route("/api/dokumenter").tag(Tags.Dokumenter) {
        route("/bruker").post<Unit, List<Journalpost>, HentDokumentoversiktBrukerRequest> { _, req ->
            val dokumenter = SafClient.hentDokumenterForBruker(req.personIdent, token())

            respond(dokumenter)
        }

        route("/sak/{saksnummer}").get<HentDokumentoversiktFagsakParams, List<Doc>> { req ->
            val dokumenter = SafClient.hentDokumenterForSak(Saksnummer(req.saksnummer), token())
                .flatMap(::mapTilDokumentliste)

            respond(dokumenter)
        }

        route("/sak/{saksnummer}/helsedokumenter").get<HentDokumentoversiktFagsakParams, List<Doc>> { req ->
            val dokumenter = SafClient.hentDokumenterForSak(Saksnummer(req.saksnummer), token())
                .flatMap(::mapTilDokumentliste)
                .dokumentFilterDokumentSøk()

            respond(dokumenter)
        }

        route("/{journalpostId}/{dokumentinfoId}").get<HentDokumentParams, HentDokumentResponse> { req ->
            val gateway = SafHentDokumentGateway.withDefaultRestClient()
            val response = gateway.hentDokument(req.journalpostId, req.dokumentinfoId, token())

            respond(HentDokumentResponse(response.dokument))
        }

        route("/{journalpostId}/knyttTilAnnenSak").post<JournalpostIdParams, KnyttTilAnnenSakResponse, KnyttTilAnnenSakRequest> { params, req ->
            val gateway = DokArkivClient(OnBehalfOfTokenProvider)

            val dokarkivResponse = gateway.knyttJournalpostTilAnnenSak(params.journalpostId, req, token())

            respond(dokarkivResponse)
        }

        route("/{journalpostId}/feilregistrer/feilregistrerSakstilknytning").post<JournalpostIdParams, String, Unit> { params, _ ->
            val gateway = DokArkivClient(OnBehalfOfTokenProvider)

            gateway.feilregistrerSakstilknytning(params.journalpostId, token())

            respond("{}", HttpStatusCode.OK)
        }

        route("/{journalpostId}/feilregistrer/opphevFeilregistrertSakstilknytning").post<JournalpostIdParams, String, Unit> { params, _ ->
            val gateway = DokArkivClient(OnBehalfOfTokenProvider)

            gateway.opphevFeilregistrertSakstilknytning(params.journalpostId, token())

            respond("{}", HttpStatusCode.OK)
        }
    }
}

class HentDokumentoversiktBrukerRequest(
    val personIdent: String,
)

class HentDokumentoversiktFagsakParams(
    @PathParam(description = "Saksnummer") val saksnummer: String,
)

data class HentDokumentParams(
    @PathParam(description = "Journalpost-ID") val journalpostId: String,
    @PathParam(description = "Dokumentinfo-ID") val dokumentinfoId: String,
)

@BinaryResponse(contentTypes = ["application/pdf"])
class HentDokumentResponse(val dokument: InputStream)

data class JournalpostIdParams(
    @PathParam(description = "Journalpost-ID") val journalpostId: String,
)
