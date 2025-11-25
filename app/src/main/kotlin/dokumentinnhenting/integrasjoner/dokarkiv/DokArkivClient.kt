package dokumentinnhenting.integrasjoner.dokarkiv

import dokumentinnhenting.http.HttpClientFactory
import dokumentinnhenting.http.OnBehalfOfTokenProvider
import dokumentinnhenting.http.TokenProvider
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest.Bruker
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import org.slf4j.LoggerFactory

class DokArkivClient(private val tokenProvider: TokenProvider = OnBehalfOfTokenProvider) {
    private val baseUrl = requiredConfigForKey("integrasjon.dokarkiv.url")
    private val scope = requiredConfigForKey("integrasjon.dokarkiv.scope")
    private val httpClient = HttpClientFactory.create()
    private val log = LoggerFactory.getLogger(DokArkivClient::class.java)

    fun endreTemaTilAAP(
        journalpostId: String,
    ): String {
        return runBlocking {
            val token = tokenProvider.getToken(scope)
            val request = OppdaterJournalPostRequest(journalpostId)

            val response = httpClient.put("$baseUrl/rest/journalpostapi/v1/journalpost/$journalpostId") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(request)
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
                throw RuntimeException("Failed to update journalpost tema: ${response.status} - ${response.bodyAsText()}")
            }

            if (response.status == HttpStatusCode.Conflict) {
                log.warn("Got HTTP 409 Conflict when updating journalpost. Attempting to parse response as expected.")
            }

            response.body<OppdaterJournalpostResponse>().journalPostId
        }
    }

    fun knyttJournalpostTilAnnenSak(
        kildeJournalpostId: String,
        request: KnyttTilAnnenSakRequest,
        currentToken: String? = null,
    ): KnyttTilAnnenSakResponse {
        return runBlocking {
            val token = tokenProvider.getToken(scope, currentToken)

            val response = httpClient.put("$baseUrl/rest/journalpostapi/v1/journalpost/$kildeJournalpostId/knyttTilAnnenSak") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(request)
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
                throw RuntimeException("Failed to link journalpost to another sak: ${response.status} - ${response.bodyAsText()}")
            }

            if (response.status == HttpStatusCode.Conflict) {
                log.warn("Got HTTP 409 Conflict. Attempting to parse response as expected.")
            }

            response.body<KnyttTilAnnenSakResponse>()
        }
    }

    fun feilregistrerSakstilknytning(
        kildeJournalpostId: String,
        currentToken: String? = null,
    ) {
        runBlocking {
            val token = tokenProvider.getToken(scope, currentToken)

            val response = httpClient.patch("$baseUrl/rest/journalpostapi/v1/journalpost/$kildeJournalpostId/feilregistrer/feilregistrerSakstilknytning") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(Unit)
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
                throw RuntimeException("Failed to feilregistrer sakstilknytning: ${response.status} - ${response.bodyAsText()}")
            }
        }
    }

    fun opphevFeilregistrertSakstilknytning(
        kildeJournalpostId: String,
        currentToken: String? = null,
    ) {
        runBlocking {
            val token = tokenProvider.getToken(scope, currentToken)

            val response = httpClient.patch("$baseUrl/rest/journalpostapi/v1/journalpost/$kildeJournalpostId/feilregistrer/opphevFeilregistrertSakstilknytning") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(Unit)
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
                throw RuntimeException("Failed to opphev feilregistrert sakstilknytning: ${response.status} - ${response.bodyAsText()}")
            }
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

data class OppdaterJournalPostRequest(
    val tema: String = "AAP",
)

data class OppdaterJournalpostResponse(
    val journalPostId: String,
)

data class KopierJournalpostResponse(
    val kopierJournalpostId: String,
)
