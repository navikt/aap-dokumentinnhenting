package dokumentinnhenting.integrasjoner.syfo.oppslag

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Duration
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class SyfoGateway {
    private val syfoUri = requiredConfigForKey("INTEGRASJON_SYFO_BASE_URL")
    private val scope = requiredConfigForKey("INTEGRASJON_SYFO_SCOPE")

    private val behandlereCache: Cache<String, List<BehandlerOppslagResponse>> = Caffeine.newBuilder()
        .maximumSize(5_000)
        .expireAfterWrite(Duration.ofHours(12))
        .build()

    suspend fun frisøkBehandlerOppslag(frisøk: String, token: OidcToken): List<BehandlerOppslagResponse> {
        return defaultHttpClient.post("$syfoUri/api/v1/behandler/search") {
            accept(ContentType.Application.Json)
            bearerAuth(OboTokenProvider.getToken(scope, token))
            contentType(ContentType.Application.Json)
            setBody(SearchRequest(frisøk))
        }.body()
    }

    suspend fun behandlere(personIdent: String, token: OidcToken): List<BehandlerOppslagResponse> =
        behandlereCache.getIfPresent(personIdent)
            ?: hentBehandlere(personIdent, token).also { behandlereCache.put(personIdent, it) }

    private suspend fun hentBehandlere(personIdent: String, token: OidcToken): List<BehandlerOppslagResponse> {
        return try {
            defaultHttpClient.get("$syfoUri/api/v1/behandler/personident") {
                accept(ContentType.Application.Json)
                bearerAuth(OboTokenProvider.getToken(scope, token))
                contentType(ContentType.Application.Json)
                headers["nav-personident"] = personIdent
            }.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                throw IkkeTillattException("Ikke tilgang til behandlere.")
            }
            throw e
        }
    }
}

data class SearchRequest(
    val searchstring: String,
)
