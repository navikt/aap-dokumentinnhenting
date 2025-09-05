package dokumentinnhenting.integrasjoner.dokarkiv

import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest.Bruker
import dokumentinnhenting.util.metrics.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import java.net.URI

class DokArkivClient(tokenProvider: TokenProvider) {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.dokarkiv.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokarkiv.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = tokenProvider,
        responseHandler = HåndterConflictResponseHandler(),
        prometheus = prometheus
    )

    fun endreTemaTilAAP(
        journalpostId: String,
    ): String {
        val uri = baseUri.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")
        val request = OppdaterJournalPostRequest(journalpostId)
        val httpRequest = PutRequest(
            body = request,
        )

        val response =
            checkNotNull(
                client.put<OppdaterJournalPostRequest, OppdaterJournalpostResponse>(
                    uri,
                    httpRequest
                )
            )

        return response.journalPostId
    }

    fun knyttJournalpostTilAnnenSak(
        kildeJournalpostId: String,
        request: KnyttTilAnnenSakRequest,
        token: OidcToken? = null,
    ): KnyttTilAnnenSakResponse {
        val uri =
            baseUri.resolve("/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/knyttTilAnnenSak")
        val httpRequest = PutRequest(body = request, currentToken = token)

        return checkNotNull(
            client.put<KnyttTilAnnenSakRequest, KnyttTilAnnenSakResponse>(
                uri,
                httpRequest
            )
        )
    }

    fun feilregistrerSakstilknytning(
        kildeJournalpostId: String,
        token: OidcToken? = null,
    ) {
        val uri =
            baseUri.resolve("/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/feilregistrer/feilregistrerSakstilknytning")
        val httpRequest = PatchRequest(body = Unit, currentToken = token)

        return checkNotNull(client.patch(uri, httpRequest) { _, _ -> })
    }

    fun opphevFeilregistrertSakstilknytning(
        kildeJournalpostId: String,
        token: OidcToken? = null,
    ) {
        val uri =
            baseUri.resolve("/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/feilregistrer/opphevFeilregistrertSakstilknytning")
        val httpRequest = PatchRequest(body = Unit, currentToken = token)

        return checkNotNull(client.patch(uri, httpRequest) { _, _ -> })
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
