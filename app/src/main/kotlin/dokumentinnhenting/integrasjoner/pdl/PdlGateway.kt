package dokumentinnhenting.integrasjoner.pdl

import dokumentinnhenting.util.metrics.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

object PdlGateway {
    val url: URI = URI.create(requiredConfigForKey("INTEGRASJON_PDL_URL"))
    val config = ClientConfig(
        scope = requiredConfigForKey("INTEGRASJON_PDL_SCOPE"),
        // https://behandlingskatalog.ansatt.nav.no/process/system/KELVIN/0ae77024-b2c8-4316-ad36-a04cde0e49d3
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287"))
    )
    val client = RestClient(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        responseHandler = PdlResponseHandler(),
        prometheus = prometheus
    )

    inline fun <reified E> query(request: PdlRequest, currentToken: OidcToken? = null): E {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }
}