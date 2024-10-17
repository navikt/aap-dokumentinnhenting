package integrasjonportal.integrasjoner.behandlingsflyt

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import integrasjonportal.util.http.HttpClientFactory
import integrasjonportal.util.metrics.cacheHit
import integrasjonportal.util.metrics.cacheMiss

private val log = LoggerFactory.getLogger(BehandlingsflytClient::class.java)

class BehandlingsflytClient(
   // private val behandlingsflytConfig: BehandlingsflytConfig,
  //  private val prometheus: PrometheusMeterRegistry
) {/* TODO: Skriv om denne til å bruke lib om den skal brukes
    private val httpClient = HttpClientFactory.create()


    suspend fun hentIdenterForBehandling(currentToken: String, behandlingsnummer: String): IdenterRespons {
        prometheus.cacheMiss(BEHANDLINGSFLYT).increment()

        val token = azureTokenProvider.getOnBehalfOfToken(currentToken)
        val url = "${behandlingsflytConfig.baseUrl}/pip/api/behandling/${behandlingsnummer}/identer"
        log.info("Kaller behandlingsflyt med URL: $url")

        val respons = httpClient.get(url) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }

        val body = respons.body<IdenterRespons>()

        return when (respons.status) {
            HttpStatusCode.OK -> {
                val identer = body
                identer
            }

            else -> throw BehandlingsflytException("Feil ved henting av identer fra behandlingsflyt: ${respons.status} : ${respons.bodyAsText()}")
        }
    }
*/
    companion object {
        private const val IDENTER_SAK_PREFIX = "identer_sak"
        private const val IDENTER_BEHANDLING_PREFIX = "identer_behandling"
        private const val BEHANDLINGSFLYT = "Behandlingsflyt"
    }
}

data class IdenterRespons(val søker: List<String>, val barn: List<String>)
