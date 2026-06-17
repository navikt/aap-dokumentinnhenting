package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.SystemTokenProvider
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import no.nav.aap.komponenter.config.requiredConfigForKey

object SafRestGateway {
    private val safBaseUrl = requiredConfigForKey("INTEGRASJON_SAF_URL_REST")
    private val scope = requiredConfigForKey("INTEGRASJON_SAF_SCOPE")

    suspend fun hentDokumentMedJournalpostId(journalpostId: String, dokumentId: String): ByteArray {
        return try {
            defaultHttpClient.get("$safBaseUrl/hentdokument/${journalpostId}/${dokumentId}/${Variantformat.ARKIV}") {
                accept(ContentType.Application.Json)
                bearerAuth(SystemTokenProvider.getToken(scope, null))
            }.body()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av dokument i saf: ${e.message}", e)
        }
    }
}
