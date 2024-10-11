package integrasjonportal.integrasjoner.behandlingsflyt

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import integrasjonportal.BehandlingsflytConfig
import integrasjonportal.util.auth.AzureAdTokenProvider
import integrasjonportal.util.auth.AzureConfig
import integrasjonportal.util.http.HttpClientFactory
import integrasjonportal.metrics.cacheHit
import integrasjonportal.metrics.cacheMiss

private val log = LoggerFactory.getLogger(BehandlingsflytClient::class.java)

class BehandlingsflytClient(
    azureConfig: AzureConfig,
    private val behandlingsflytConfig: BehandlingsflytConfig,
    private val prometheus: PrometheusMeterRegistry
) {
    private val httpClient = HttpClientFactory.create()
    private val azureTokenProvider = AzureAdTokenProvider(azureConfig, behandlingsflytConfig.scope)

    suspend fun hentIdenterForSak(currentToken: String, saksnummer: String): IdenterRespons {
        val token = azureTokenProvider.getOnBehalfOfToken(currentToken)
        prometheus.cacheMiss(BEHANDLINGSFLYT).increment()

        val url = "${behandlingsflytConfig.baseUrl}/pip/api/sak/${saksnummer}/identer"
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

    companion object {
        private const val IDENTER_SAK_PREFIX = "identer_sak"
        private const val IDENTER_BEHANDLING_PREFIX = "identer_behandling"
        private const val BEHANDLINGSFLYT = "Behandlingsflyt"
    }
}

data class IdenterRespons(val søker: List<String>, val barn: List<String>)
