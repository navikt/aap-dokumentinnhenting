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
import dokumentinnhenting.integrasjoner.saf.*
import dokumentinnhenting.util.Tags
import dokumentinnhenting.util.dokument.dokumentFilterDokumentSøk
import dokumentinnhenting.util.dokument.mapKunVariantformatArkiv
import dokumentinnhenting.util.dokument.mapTilDokumentliste
import io.ktor.http.*
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.komponenter.server.auth.token
import java.io.InputStream

fun NormalOpenAPIRoute.dokumentApi() {
    route("/api/dokumenter").tag(Tags.Dokumenter) {
        route("/bruker").post<Unit, List<Journalpost>, HentDokumentoversiktBrukerRequest> { _, req ->
            val dokumenter = SafClient.hentDokumenterForBruker(
                ident = req.personIdent,
                tema = req.tema,
                typer = req.typer,
                statuser = req.statuser,
                token = token()
            )

            respond(dokumenter)
        }

        route("/bruker/helsedokumenter").post<Unit, List<Doc>, HentDokumentoversiktBrukerRequest> { _, req ->
            val saksnummer = requireNotNull(req.saksnummer)

            val dokumenter = SafClient.hentDokumenterForBruker(
                ident = req.personIdent,
                tema = listOf("AAP", "OPP", "SYK", "SYM"),
                statuser = listOf(Journalstatus.JOURNALFOERT, Journalstatus.FERDIGSTILT),
                token = token()
            )
                .filterNot { it.sak?.fagsakId == saksnummer }
                .flatMap(::mapTilDokumentliste)
                .dokumentFilterDokumentSøk()

            respond(dokumenter)
        }

        route("/sak/{saksnummer}").get<HentDokumentoversiktFagsakParams, List<Journalpost>> { req ->
            val journalposter = SafClient.hentDokumenterForSak(Saksnummer(req.saksnummer), token())
                .mapKunVariantformatArkiv()

            respond(journalposter)
        }

        route("/{journalpostId}/{dokumentinfoId}").get<HentDokumentParams, HentDokumentResponse> { req ->
            val gateway = SafHentDokumentGateway.withDefaultRestClient()
            val response = gateway.hentDokument(req.journalpostId, req.dokumentinfoId, token())

            respond(HentDokumentResponse(response.dokument))
        }

        route("/{journalpostId}/knyttTilAnnenSak").post<JournalpostIdParams, KnyttTilAnnenSakResponse, KnyttTilAnnenSakRequest> { params, req ->
            val gateway = DokArkivClient(OnBehalfOfTokenProvider)

            val dokarkivResponse =
                gateway.knyttJournalpostTilAnnenSak(params.journalpostId, req, token())

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

data class HentDokumentoversiktBrukerRequest(
    val personIdent: String,
    val saksnummer: String? = null,
    val tema: List<String> = listOf("AAP"),
    val typer: List<Journalposttype> = emptyList(),
    val statuser: List<Journalstatus> = emptyList(),
)

class HentDokumentoversiktFagsakParams(
    @param:PathParam(description = "Saksnummer") val saksnummer: String,
)

data class HentDokumentParams(
    @param:PathParam(description = "Journalpost-ID") val journalpostId: String,
    @param:PathParam(description = "Dokumentinfo-ID") val dokumentinfoId: String,
)

@BinaryResponse(contentTypes = ["application/pdf"])
class HentDokumentResponse(val dokument: InputStream)

data class JournalpostIdParams(
    @param:PathParam(description = "Journalpost-ID") val journalpostId: String,
)
