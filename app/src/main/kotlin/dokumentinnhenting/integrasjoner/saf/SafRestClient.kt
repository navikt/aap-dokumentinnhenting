package dokumentinnhenting.integrasjoner.saf

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI

class SafRestClient {
    private val restUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.rest"))

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = SafClient.config,
        tokenProvider = OnBehalfOfTokenProvider
    )

    fun hentDokumentMedJournalpostId(journalpostId: String, dokumentId: String): ByteArray {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        try {
            return requireNotNull(client.get(
                uri = URI.create("$restUrl/hentdokument/${journalpostId}/${dokumentId}/${Variantformat.ORIGINAL}"),
                request = request,
                mapper = { body, _ -> DefaultJsonMapper.fromJson(body)} ))
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av dokument i saf: ${e.message}")
        }
    }
}