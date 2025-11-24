package dokumentinnhenting.integrasjoner.syfo.oppslag

import dokumentinnhenting.util.metrics.prometheus
import java.net.URI
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory

object SyfoGateway {
    private val secureLogger = LoggerFactory.getLogger("secureLog")

    private val syfoUri = requiredConfigForKey("integrasjon.syfo.base.url")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.syfo.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    fun frisøkBehandlerOppslag(fritekst: String): List<BehandlerOppslagResponse> {
        secureLogger.info("Mottatt forespørsel om frisøk behandleroppslag med fritekst='$fritekst'")

        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("searchstring", fritekst)
            )
        )

        try {
            return requireNotNull(
                client.get(
                    uri = URI.create("$syfoUri/api/v1/behandler/search"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) })
            )
        } catch (e: Exception) {
            throw RuntimeException("Feil ved oppslag av behandler i syfo: ${e.message}")
        }
    }
}
