package dokumentinnhenting.integrasjoner.azure

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.jackson
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig

internal object AzureAdGateway {
    private val config = AzureConfig()
    private val httpClient: HttpClient = azureHttpClient

    private val azureClientId = config.clientId
    private val azureClientSecret = config.clientSecret
    private val azureTokenEndpoint = config.tokenEndpoint.toString()

    private val mutex = Mutex()

    private val tokens: HashMap<String, AzureAdToken> = hashMapOf()

    suspend fun getOboToken(scope: String, token: OidcToken): AzureAdToken {
        if (token.isClientCredentials()) return getSystemToken(scope)

        return mutex.withLock {
            tokens["$scope:${token.navIdent()}"]
                ?.takeUnless { it.isExpired() }
                ?: getToken(
                    Parameters.build {
                        append("client_id", azureClientId)
                        append("client_secret", azureClientSecret)
                        append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("assertion", token.token())
                        append("scope", scope)
                        append("requested_token_use", "on_behalf_of")
                    }
                ).also { tokens[scope] = it }
        }
    }

    suspend fun getSystemToken(scope: String): AzureAdToken {
        return mutex.withLock {
            tokens[scope]
                ?.takeUnless { it.isExpired() }
                ?: getToken(
                    Parameters.build {
                        append("client_id", azureClientId)
                        append("client_secret", azureClientSecret)
                        append("scope", scope)
                        append("grant_type", "client_credentials")
                    }
                ).also { tokens[scope] = it }
        }
    }

    private suspend fun getToken(formParameters: Parameters): AzureAdToken {
        return httpClient.submitForm(azureTokenEndpoint, formParameters) {
            accept(ContentType.Application.Json)
        }.body()
    }
}

data class AzureAdToken(
    @field:JsonProperty("access_token")
    val accessToken: String,
    @field:JsonProperty("expires_in")
    private val expiresIn: Long,
) {
    private val expires: Instant = Instant.now().plusSeconds(expiresIn - LEEWAY_SECONDS)

    fun isExpired() = Instant.now().isAfter(expires)

    companion object {
        private const val LEEWAY_SECONDS = 60
    }
}

private val azureHttpClient = HttpClient(CIO) {
    expectSuccess = true
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    install(HttpRequestRetry) {
        retryOnException(maxRetries = 3)
        exponentialDelay()
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 2.seconds.inWholeMilliseconds
        requestTimeoutMillis = 10.seconds.inWholeMilliseconds
    }
}
