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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import org.slf4j.LoggerFactory

internal object AzureAdGateway {
    private val config = AzureConfig()
    private val httpClient: HttpClient = azureHttpClient

    private val azureClientId = config.clientId
    private val azureClientSecret = config.clientSecret
    private val azureTokenEndpoint = config.tokenEndpoint.toString()

    private val mutex = Mutex()

    private val tokens: HashMap<String, AzureAdToken> = hashMapOf()
    private val secureLog = LoggerFactory.getLogger("secureLog")

    suspend fun getOboToken(scope: String, token: String): AzureAdToken {
        return mutex.withLock {
            tokens[scope]
                ?.takeUnless { it.isExpired() }
                ?: getToken(
                    Parameters.build {
                        append("client_id", azureClientId)
                        append("client_secret", azureClientSecret)
                        append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("assertion", token)
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
        val res: HttpResponse = httpClient.submitForm(azureTokenEndpoint, formParameters) {
            accept(ContentType.Application.Json)
        }

        if (!res.status.isSuccess()) {
            secureLog.warn("Feilet token-kall {}: {}", res.status.value, res.bodyAsText())
        }
        return res.body<AzureAdToken>()
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
        retryOnException(maxRetries = 3) // on exceptions during network send, other than timeouts
        exponentialDelay()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5_000
    }
}
