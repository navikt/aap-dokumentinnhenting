package dokumentinnhenting.integrasjoner.syfo.oppslag

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI

class SyfoGateway {
    private val syfoUri = requiredConfigForKey("integrasjon.syfo.base.url" + "/api/v1/behandler")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.syfo.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun frisøkBehandlerOppslag(frisøk: String): List<BehandlerOppslagResponse> {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
                Header("searchstring", frisøk)
            )
        )

        try {
            return requireNotNull(client.get(uri = URI.create("$syfoUri/search"), request = request, mapper = { body, _ -> DefaultJsonMapper.fromJson(body)} ))
        } catch (e : Exception) {
            throw RuntimeException("Feil ved oppslag av behandler i syfo: ${e.message}")
        }
    }
}
