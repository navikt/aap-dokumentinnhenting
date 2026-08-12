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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlin.time.Duration.Companion.seconds
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.server.auth.IdentityProvider

internal object TexasGateway {
    private val texasM2mUri = requiredConfigForKey("nais.token.endpoint")
    private val texasOboUri = requiredConfigForKey("nais.token.exchange.endpoint")

    private val httpClient: HttpClient = azureHttpClient

    suspend fun getOboToken(scope: String, token: OidcToken): TexasToken {
        require(!token.isClientCredentials()) {
            "OboToken skal ikke brukes for systembruker (client credentials)"
        }

        return httpClient.post(texasOboUri) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "identity_provider" to IdentityProvider.ENTRA_ID.value,
                    "target" to scope,
                    "user_token" to token.token(),
                )
            )
        }.body<TexasToken>()
    }

    suspend fun getSystemToken(scope: String): TexasToken {
        return httpClient.post(texasM2mUri) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "identity_provider" to IdentityProvider.ENTRA_ID.value,
                    "target" to scope,
                )
            )
        }.body<TexasToken>()
    }

}

internal data class TexasToken(
    @param:JsonProperty("access_token")
    val accessToken: String,
    @param:JsonProperty("expires_in")
    private val expiresIn: Long,
)

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
        socketTimeoutMillis = 10.seconds.inWholeMilliseconds
    }
}
