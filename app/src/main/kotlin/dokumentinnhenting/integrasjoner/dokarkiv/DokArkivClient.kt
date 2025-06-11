package dokumentinnhenting.integrasjoner.dokarkiv

import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest.Bruker
import java.net.URI
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider

class DokArkivClient(tokenProvider: TokenProvider ) {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokarkiv.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokarkiv.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = tokenProvider,
        responseHandler = HåndterConflictResponseHandler()
    )

    fun endreTemaTilAAP(
        journalpostId: String
    ): String {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")
        val request = OppdaterJournalPostRequest(journalpostId)
        val httpRequest = PutRequest(
            body = request,
        )

        val response =
            checkNotNull(client.put<OppdaterJournalPostRequest, OppdaterJournalpostResponse>(uri, httpRequest))

        return response.journalPostId
    }

    fun knyttJournalpostTilAnnenSak(
        kildeJournalpostId: String,
        bruker: Bruker,
        fagsakId: String,
        fagsaksystem: String
    ):KnyttTilAnnenSakResponse {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/knyttTilAnnenSak")
        val request = KnyttTilAnnenSakRequest(
            bruker = bruker,
            fagsakId = fagsakId,
            fagsaksystem = fagsaksystem,
        )
        val httpRequest = PutRequest(
            body = request,
        )

        return checkNotNull(client.put<KnyttTilAnnenSakRequest, KnyttTilAnnenSakResponse>(uri, httpRequest))
    }
}

data class KnyttTilAnnenSakRequest(
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

data class OppdaterJournalPostRequest(
    val tema: String = "AAP",
)

data class OppdaterJournalpostResponse(
    val journalPostId: String,
)

data class KopierJournalpostResponse(
    val kopierJournalpostId: String,
)
