package dokumentinnhenting.integrasjoner.dokarkiv

import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest.*
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.*
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

class DokArkivClient(tokenProvider:TokenProvider){
    private val log = LoggerFactory.getLogger(DokArkivClient::class.java)


    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokarkiv.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokarkiv.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = tokenProvider,
        responseHandler = HåndterConflictResponseHandler()
    )

    fun kopierJournalpostForDialogMelding(
        eksternReferanseId: String,
        journalPostId: String
    ): String {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/kopierJournalpost?kildeJournalpostId=$journalPostId")
        val request = kopierJournalpostRequest(eksternReferanseId = eksternReferanseId)
        val httpRequest = PostRequest(
            body = request,

        )
        val response =
            checkNotNull(client.post<kopierJournalpostRequest, kopierJournalpostResponse>(uri, httpRequest))


        return response.kopierJournalpostId
    }

    fun endreTemaTilAAP(
        journalpostId:String
    ):String{
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")
        val request = oppdaterJournalPostRequest(journalpostId)
        val httpRequest = PutRequest(
            body = request,
        )

        val response =
            checkNotNull(client.put<oppdaterJournalPostRequest, OppdaterJournalpostResponse>(uri, httpRequest))

        return response.journalPostId
    }

    fun knyttJournalpostTilAnnenSak(
        kildeJournalpostId: String,
        bruker: Bruker,
        fagsakId: String,
        fagsaksystem: String
    ){
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/knyttTilAnnenSak")
        val request = knyttTilAnnenSakRequest(
            bruker = bruker,
            fagsakId = fagsakId,
            fagsaksystem = fagsaksystem,
        )
        val httpRequest = PutRequest(
            body = request,
        )

        val response = checkNotNull(client.put<knyttTilAnnenSakRequest, KnyttTilAnnenSakResponse>(uri, httpRequest))
    }

    fun ferdigstillJournalpost(journalpostId: String) {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill")
        val request = FerdigstillJournalpostRequest(journalfoerendeEnhet = "9999")
        val httpRequest = PatchRequest(
            body = request,
        )
        client.patch<FerdigstillJournalpostRequest, Unit>(uri, httpRequest)
    }
}

class FerdigstillJournalpostRequest(journalfoerendeEnhet: String)

data class knyttTilAnnenSakRequest(
    val bruker: Bruker,
    val fagsakId: String,
    val fagsaksystem: String,
    val journalfoerendeEnhet: String = "9999",
    val sakstype: String = "FAGSAK",
    val tema: String = "AAP",
)

data class KnyttTilAnnenSakResponse(
    val nyJournalpostId: String,
)

data class oppdaterJournalPostRequest(
    val tema: String="AAP",
)

data class OppdaterJournalpostResponse(
    val journalPostId: String,
)

data class kopierJournalpostRequest(
    val eksternReferanseId: String,
)

data class kopierJournalpostResponse(
    val kopierJournalpostId: String,
)