package dokumentinnhenting.api

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.model.operation.MediaTypeModel
import com.papsign.ktor.openapigen.model.operation.OperationModel
import com.papsign.ktor.openapigen.model.operation.StatusResponseModel
import com.papsign.ktor.openapigen.model.schema.DataType
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.RouteOpenAPIModule
import com.papsign.ktor.openapigen.modules.openapi.OperationModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import dokumentinnhenting.integrasjoner.dokarkiv.DokarkivGateway
import dokumentinnhenting.integrasjoner.dokarkiv.KnyttTilAnnenSakRequest
import dokumentinnhenting.integrasjoner.dokarkiv.KnyttTilAnnenSakResponse
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.Journalposttype
import dokumentinnhenting.integrasjoner.saf.Journalstatus
import dokumentinnhenting.integrasjoner.saf.SafGateway
import dokumentinnhenting.integrasjoner.saf.SafHentDokumentGateway
import dokumentinnhenting.integrasjoner.saf.Saksnummer
import dokumentinnhenting.integrasjoner.saf.Utsendingsinfo
import dokumentinnhenting.integrasjoner.saf.Variantformat
import dokumentinnhenting.util.Tags
import dokumentinnhenting.util.dokument.dokumentFilterDokumentSøk
import dokumentinnhenting.util.dokument.mapKunVariantformatArkiv
import dokumentinnhenting.util.dokument.mapTilDokumentliste
import dokumentinnhenting.util.dokument.tilBegrensetDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.charset
import io.ktor.server.response.respondText
import java.io.InputStream
import no.nav.aap.dokumentinnhenting.kontrakt.HentDokumentoversiktJournalpostListeResponse
import no.nav.aap.dokumentinnhenting.kontrakt.HentDokumentoversiktJournalpostResponse
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.verdityper.dokument.JournalpostId

fun NormalOpenAPIRoute.dokumentApi(dokarkivGateway: DokarkivGateway) {
    route("/api/dokumenter").tag(Tags.Dokumenter) {
        route("/bruker").post<Unit, List<Journalpost>, HentDokumentoversiktBrukerRequest> { _, req ->
            val dokumenter = SafGateway.hentDokumenterForBruker(
                ident = req.personIdent,
                tema = req.tema,
                typer = req.typer,
                statuser = req.statuser,
                token = token()
            )

            respond(dokumenter)
        }

        data class HelsedokumenterRequest(val saksnummer: String, val personIdent: String)

        route("/bruker/helsedokumenter").post<Unit, List<Doc>, HelsedokumenterRequest> { _, req ->
            val saksnummer = req.saksnummer

            val dokumenter = SafGateway.hentDokumenterForBruker(
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
            val journalposter = SafGateway.hentDokumenterForSak(Saksnummer(req.saksnummer), token())
                .mapKunVariantformatArkiv()

            respond(journalposter)
        }

        route("/dokumentliste").post<Unit, HentDokumentoversiktJournalpostListeResponse, HentDokumentoversiktJournalpostListeParams> { _, params ->
            val journalposter = params.journalpostIdListe.map { journalpostId ->
                SafGateway.hentDokumenterForJournalpost(JournalpostId(journalpostId), token())
            }

            val journalposterBegrenset = journalposter.filterNotNull().map { it.tilBegrensetDto() }

            respond(HentDokumentoversiktJournalpostListeResponse(journalposterBegrenset))
        }

        route("/{journalpostId}/dokumentliste").get<HentDokumentoversiktJournalpostParams, HentDokumentoversiktJournalpostResponse> { params ->
            val journalpost = SafGateway.hentDokumenterForJournalpost(JournalpostId(params.journalpostId), token())

            respond(HentDokumentoversiktJournalpostResponse(journalpost?.tilBegrensetDto()))
        }

        route("/{journalpostId}/{dokumentinfoId}").get<HentDokumentParams, HentDokumentResponse> { req ->
            val response = SafHentDokumentGateway.hentDokument(req.journalpostId, req.dokumentinfoId, token())

            respond(HentDokumentResponse(response.dokument))
        }

        route("/{journalpostId}/{dokumentinfoId}/ORIGINAL").get<HentDokumentParams, Unit>(OriginalDokumentResponse) { req ->
            val response = SafHentDokumentGateway.hentDokument(req.journalpostId, req.dokumentinfoId, token(), Variantformat.ORIGINAL)

            val contentType = ContentType.parse(response.contentType)
            val text = response.dokument.bufferedReader(contentType.charset() ?: Charsets.UTF_8).use {
                it.readText()
            }
            val responseContentType = if (contentType.match(ContentType.Application.Json)) {
                contentType
            } else {
                ContentType.Text.Plain
            }
            pipeline.call.respondText(text, responseContentType)
        }

        route("/{journalpostId}/utsendingsinfo").get<JournalpostIdParams, HentUtsendingsinfoResponse> { params ->
            val utsendingsinfo = SafGateway.hentUtsendingsinfo(JournalpostId(params.journalpostId), token())

            respond(HentUtsendingsinfoResponse(utsendingsinfo))
        }

        route("/{journalpostId}/knyttTilAnnenSak").post<JournalpostIdParams, KnyttTilAnnenSakResponse, KnyttTilAnnenSakRequest> { params, req ->
            val dokarkivResponse =
                dokarkivGateway.knyttJournalpostTilAnnenSak(params.journalpostId, req, token())

            respond(dokarkivResponse)
        }

        route("/{journalpostId}/feilregistrer/feilregistrerSakstilknytning").post<JournalpostIdParams, String, Unit> { params, _ ->
            dokarkivGateway.feilregistrerSakstilknytning(params.journalpostId, token())

            respond("{}", HttpStatusCode.OK)
        }

        route("/{journalpostId}/feilregistrer/opphevFeilregistrertSakstilknytning").post<JournalpostIdParams, String, Unit> { params, _ ->
            dokarkivGateway.opphevFeilregistrertSakstilknytning(params.journalpostId, token())

            respond("{}", HttpStatusCode.OK)
        }
    }
}

data class HentDokumentoversiktBrukerRequest(
    val personIdent: String,
    val tema: List<String> = listOf("AAP"),
    val typer: List<Journalposttype> = emptyList(),
    val statuser: List<Journalstatus> = emptyList(),
)

data class HentDokumentoversiktFagsakParams(
    @param:PathParam(description = "Saksnummer") val saksnummer: String,
)

data class HentDokumentParams(
    @param:PathParam(description = "Journalpost-ID") val journalpostId: String,
    @param:PathParam(description = "Dokumentinfo-ID") val dokumentinfoId: String,
)

data class HentDokumentoversiktJournalpostParams(@param:PathParam(description = "Journalpost-ID") val journalpostId: String)

data class HentDokumentoversiktJournalpostListeParams(val journalpostIdListe: List<String>)

@BinaryResponse(contentTypes = ["application/pdf"])
class HentDokumentResponse(val dokument: InputStream)

private object OriginalDokumentResponse : RouteOpenAPIModule, OperationModule {
    override fun configure(apiGen: OpenAPIGen, provider: ModuleProvider<*>, operation: OperationModel) {
        operation.responses["200"] = StatusResponseModel(
            description = HttpStatusCode.OK.description,
            content = mutableMapOf(
                "application/json" to MediaTypeModel(SchemaModel.SchemaModelLitteral<Any>(nullable = true)),
                "text/plain" to MediaTypeModel(SchemaModel.SchemaModelLitteral<String>(type = DataType.string)),
            )
        )
    }
}

class HentUtsendingsinfoResponse(val utsendingsinfo: Utsendingsinfo?)

data class JournalpostIdParams(
    @param:PathParam(description = "Journalpost-ID") val journalpostId: String,
)
