package dokumentinnhenting.integrasjoner.syfo.oppslag

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.aap.komponenter.config.requiredConfigForKey

class SyfoGateway {
    private val syfoUri = requiredConfigForKey("integrasjon.syfo.base.url")
    private val scope = requiredConfigForKey("integrasjon.syfo.scope")

    suspend fun frisøkBehandlerOppslag(frisøk: String, token: String): List<BehandlerOppslagResponse> {
        return try {
            defaultHttpClient.post("$syfoUri/api/v1/behandler/search") {
                accept(ContentType.Application.Json)
                bearerAuth(OboTokenProvider.getToken(scope, token))
                contentType(ContentType.Application.Json)
                setBody(SearchRequest(frisøk))
            }.body()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved oppslag av behandler i syfo: ${e.message}")
        }
    }
}

data class SearchRequest(
    val searchstring: String,
)
