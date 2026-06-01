package dokumentinnhenting.integrasjoner.syfo.oppslag

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.Duration
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

class SyfoGateway {
    private val syfoUri = requiredConfigForKey("integrasjon.syfo.base.url")
    private val scope = requiredConfigForKey("integrasjon.syfo.scope")

    private val behandlereCache: Cache<String, List<BehandlerOppslagResponse>> = Caffeine.newBuilder()
        .maximumSize(5_000)
        .expireAfterWrite(Duration.ofHours(12))
        .build()

    suspend fun frisøkBehandlerOppslag(frisøk: String, token: OidcToken): List<BehandlerOppslagResponse> {
        return try {
            defaultHttpClient.post("$syfoUri/api/v1/behandler/search") {
                accept(ContentType.Application.Json)
                bearerAuth(OboTokenProvider.getToken(scope, token))
                contentType(ContentType.Application.Json)
                setBody(SearchRequest(frisøk))
            }.body()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved søk på behandler i syfo: ${e.message}")
        }
    }

    suspend fun behandlere(personIdent: String, token: OidcToken): List<BehandlerOppslagResponse> =
        behandlereCache.getIfPresent(personIdent)
            ?: hentBehandlere(personIdent, token).also { behandlereCache.put(personIdent, it) }

    private suspend fun hentBehandlere(personIdent: String, token: OidcToken): List<BehandlerOppslagResponse> =
        try {
            defaultHttpClient.get("$syfoUri/api/v1/behandler/personident") {
                accept(ContentType.Application.Json)
                bearerAuth(OboTokenProvider.getToken(scope, token))
                contentType(ContentType.Application.Json)
                headers["nav-personident"] = personIdent
            }.body()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved oppslag av behandlere i syfo: ${e.message}")
        }
}

data class SearchRequest(
    val searchstring: String,
)
