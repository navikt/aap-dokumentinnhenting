package dokumentinnhenting.integrasjoner.dokarkiv

import dokumentinnhenting.integrasjoner.azure.TokenProviderV2
import dokumentinnhenting.integrasjoner.azure.defaultHttpClient
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest.Bruker
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class DokarkivGateway(private val tokenProvider: TokenProviderV2) {
    private val baseUri = requiredConfigForKey("integrasjon.dokarkiv.url")
    private val scope = requiredConfigForKey("integrasjon.dokarkiv.scope")

    suspend fun knyttJournalpostTilAnnenSak(
        kildeJournalpostId: String,
        request: KnyttTilAnnenSakRequest,
        token: OidcToken? = null,
    ): KnyttTilAnnenSakResponse {
        return defaultHttpClient.put("$baseUri/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/knyttTilAnnenSak") {
            bearerAuth(tokenProvider.getToken(scope, token?.token()))
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun feilregistrerSakstilknytning(
        kildeJournalpostId: String,
        token: OidcToken? = null,
    ) {
        defaultHttpClient.patch("$baseUri/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/feilregistrer/feilregistrerSakstilknytning") {
            bearerAuth(tokenProvider.getToken(scope, token?.token()))
        }
    }

    suspend fun opphevFeilregistrertSakstilknytning(
        kildeJournalpostId: String,
        token: OidcToken? = null,
    ) {
        defaultHttpClient.patch("$baseUri/rest/journalpostapi/v1/journalpost/${kildeJournalpostId}/feilregistrer/opphevFeilregistrertSakstilknytning") {
            bearerAuth(tokenProvider.getToken(scope, token?.token()))
        }
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
