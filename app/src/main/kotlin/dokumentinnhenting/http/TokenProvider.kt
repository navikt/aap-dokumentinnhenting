package dokumentinnhenting.http

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import no.nav.aap.komponenter.config.requiredConfigForKey
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.Charsets.UTF_8

interface TokenProvider {
    suspend fun getToken(scope: String, currentToken: String? = null): String
}

data class AzureConfig(
    val tokenEndpoint: URI = URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
    val clientId: String = requiredConfigForKey("azure.app.client.id"),
    val clientSecret: String = requiredConfigForKey("azure.app.client.secret"),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

data class CachedToken(
    val token: String,
    val expiresAt: LocalDateTime
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt.minusSeconds(30))
}

object ClientCredentialsTokenProvider : TokenProvider {
    private val log = LoggerFactory.getLogger(ClientCredentialsTokenProvider::class.java)
    private val config = AzureConfig()
    private val cache = ConcurrentHashMap<String, CachedToken>()

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
    }

    override suspend fun getToken(scope: String, currentToken: String?): String {
        val cached = cache[scope]
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached token for scope: $scope")
            return cached.token
        }

        log.info("Fetching new token for scope: $scope")
        val response = httpClient.post(config.tokenEndpoint.toURL()) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.FormUrlEncoded)
            header("Cache-Control", "no-cache")
            setBody(buildClientCredentialsBody(scope))
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw RuntimeException("Failed to get token: ${response.status} - $errorBody")
        }

        val tokenResponse: TokenResponse = response.body()
        val expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.expires_in.toLong())
        cache[scope] = CachedToken(tokenResponse.access_token, expiresAt)

        return tokenResponse.access_token
    }

    private fun buildClientCredentialsBody(scope: String): String {
        val encodedScope = URLEncoder.encode(scope, UTF_8)
        return "client_id=${config.clientId}&client_secret=${config.clientSecret}&scope=$encodedScope&grant_type=client_credentials"
    }
}

object OnBehalfOfTokenProvider : TokenProvider {
    private val log = LoggerFactory.getLogger(OnBehalfOfTokenProvider::class.java)
    private val config = AzureConfig()
    private val secureLog = LoggerFactory.getLogger("secureLog")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
    }

    override suspend fun getToken(scope: String, currentToken: String?): String {
        requireNotNull(currentToken) { "Cannot request OBO token without a current token to exchange" }

        if (isClientCredentialsToken(currentToken)) {
            log.debug("Current token is client credentials, using ClientCredentialsTokenProvider")
            return ClientCredentialsTokenProvider.getToken(scope, null)
        }

        log.info("Exchanging token for scope: $scope")
        val response = httpClient.post(config.tokenEndpoint.toURL()) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.FormUrlEncoded)
            header("Cache-Control", "no-cache")
            setBody(buildOnBehalfOfBody(scope, currentToken))
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            secureLog.warn("Failed to exchange token: ${response.status} - $errorBody")
            throw RuntimeException("Failed to exchange token: ${response.status}")
        }

        val tokenResponse: TokenResponse = response.body()
        return tokenResponse.access_token
    }

    private fun buildOnBehalfOfBody(scope: String, token: String): String {
        val encodedScope = URLEncoder.encode(scope, UTF_8)
        return "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" +
            "&client_id=${config.clientId}" +
            "&client_secret=${config.clientSecret}" +
            "&assertion=$token" +
            "&scope=$encodedScope" +
            "&requested_token_use=on_behalf_of"
    }

    private fun isClientCredentialsToken(token: String): Boolean {
        return try {
            val jwt = SignedJWT.parse(token)
            val claims = jwt.jwtClaimsSet
            val roles = claims.getStringListClaim("roles")
            val idtyp = claims.getStringClaim("idtyp")
            !roles.isNullOrEmpty() || idtyp == "app"
        } catch (e: Exception) {
            false
        }
    }
}
