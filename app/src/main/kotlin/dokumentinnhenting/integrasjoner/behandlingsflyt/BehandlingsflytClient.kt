package dokumentinnhenting.integrasjoner.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattHendelseDto
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI
class BehandlingsflytClient {
    private val uri = requiredConfigForKey("behandlingsflyt.base.url")
    private val config = ClientConfig(scope = requiredConfigForKey("behandlingsflyt.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun taSakAvVent(taAvVentRequest: MottattHendelseDto) {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = taAvVentRequest
        )

        try {
            return requireNotNull(client.post(uri = URI.create("$uri/api/hendelse/send"), request = request, mapper = { body, _ -> DefaultJsonMapper.fromJson(body)} ))
        } catch (e : Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å ta sak av vent i behandlingsflyt: ${e.message}")
        }
    }
}
