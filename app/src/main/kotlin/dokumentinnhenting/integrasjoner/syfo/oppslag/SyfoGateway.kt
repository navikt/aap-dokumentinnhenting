package dokumentinnhenting.integrasjoner.syfo.oppslag

import dokumentinnhenting.http.ClientCredentialsTokenProvider
import dokumentinnhenting.http.HttpClientFactory
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey

class SyfoGateway {
    private val syfoUrl = requiredConfigForKey("integrasjon.syfo.base.url")
    private val scope = requiredConfigForKey("integrasjon.syfo.scope")
    private val httpClient = HttpClientFactory.create()

    fun frisøkBehandlerOppslag(frisøk: String): List<BehandlerOppslagResponse> {
        return runBlocking {
            try {
                val token = ClientCredentialsTokenProvider.getToken(scope)
                val response = httpClient.get("$syfoUrl/api/v1/behandler/search") {
                    accept(ContentType.Application.Json)
                    bearerAuth(token)
                    header("searchstring", frisøk)
                }

                if (!response.status.isSuccess()) {
                    throw RuntimeException("Feil ved oppslag av behandler i syfo: ${response.status} - ${response.bodyAsText()}")
                }

                response.body<List<BehandlerOppslagResponse>>()
            } catch (e: Exception) {
                throw RuntimeException("Feil ved oppslag av behandler i syfo: ${e.message}")
            }
        }
    }
}
