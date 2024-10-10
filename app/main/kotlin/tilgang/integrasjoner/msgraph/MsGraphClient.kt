package tilgang.integrasjoner.msgraph

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import tilgang.MsGraphConfig
import tilgang.auth.AzureAdTokenProvider
import tilgang.auth.AzureConfig
import tilgang.http.HttpClientFactory
import tilgang.metrics.cacheHit
import tilgang.metrics.cacheMiss
import tilgang.redis.Key
import tilgang.redis.Redis
import tilgang.redis.Redis.Companion.deserialize
import tilgang.redis.Redis.Companion.serialize

interface IMsGraphClient {
    suspend fun hentAdGrupper(currentToken: String, ident: String): MemberOf
}

class MsGraphClient(
    azureConfig: AzureConfig,
    private val msGraphConfig: MsGraphConfig,
    private val redis: Redis,
    private val prometheus: PrometheusMeterRegistry
) : IMsGraphClient {
    private val httpClient = HttpClientFactory.create()
    private val azureTokenProvider = AzureAdTokenProvider(
        azureConfig,
        msGraphConfig.scope,
    )

    override suspend fun hentAdGrupper(currentToken: String, ident: String): MemberOf {
        val graphToken = azureTokenProvider.getOnBehalfOfToken(currentToken)

        if (redis.exists(Key(MSGRAPH_PREFIX, ident))) {
            prometheus.cacheHit(MSGRAPH_PREFIX).increment()
            return redis[Key(MSGRAPH_PREFIX, ident)]!!.deserialize()
        }
        prometheus.cacheMiss(MSGRAPH_PREFIX).increment()

        val respons = httpClient.get("${msGraphConfig.baseUrl}/me/memberOf") {
            bearerAuth(graphToken)
            contentType(ContentType.Application.Json)
        }

        return when (respons.status) {
            HttpStatusCode.OK -> {
                val memberOf = respons.body<MemberOf>()
                redis.set(Key(MSGRAPH_PREFIX, ident), memberOf.serialize())
                respons.body()
            }

            else -> throw MsGraphException("Feil fra Microsoft Graph: ${respons.status} : ${respons.bodyAsText()}")
        }
    }

    companion object {
        private const val MSGRAPH_PREFIX = "msgraph"
    }
}

data class MemberOf(
    @JsonProperty("value")
    val groups: List<Group> = emptyList()
)

data class Group(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("mailNickname")
    val name: String
)