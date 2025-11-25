package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.http.ClientCredentialsTokenProvider
import dokumentinnhenting.http.HttpClientFactory
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey

class SafRestClient {
    private val restUrl = requiredConfigForKey("integrasjon.saf.url.rest")
    private val scope = requiredConfigForKey("integrasjon.saf.scope")
    private val httpClient = HttpClientFactory.create()

    fun hentDokumentMedJournalpostId(journalpostId: String, dokumentId: String): ByteArray {
        return runBlocking {
            try {
                val token = ClientCredentialsTokenProvider.getToken(scope)
                val response = httpClient.get("$restUrl/hentdokument/$journalpostId/$dokumentId/${Variantformat.ARKIV}") {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                }

                if (!response.status.isSuccess()) {
                    throw RuntimeException("Feil ved henting av dokument i saf: ${response.status} - ${response.bodyAsText()}")
                }

                response.readRawBytes()
            } catch (e: Exception) {
                throw RuntimeException("Feil ved henting av dokument i saf: ${e.message}", e)
            }
        }
    }
}