package no.nav.aap.postmottak.saf.graphql

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import tilgang.LOGGER
import tilgang.SafConfig
import tilgang.auth.AzureAdTokenProvider
import tilgang.auth.AzureConfig
import tilgang.http.HttpClientFactory
import tilgang.integrasjoner.saf.SafException
import tilgang.metrics.cacheHit
import tilgang.metrics.cacheMiss
import tilgang.redis.Key
import tilgang.redis.Redis
import tilgang.redis.Redis.Companion.deserialize
import tilgang.redis.Redis.Companion.serialize

class SafGraphqlClient(
    azureConfig: AzureConfig,
    private val safConfig: SafConfig,
    private val redis: Redis,
    private val prometheus: PrometheusMeterRegistry
) {
    private val log = LoggerFactory.getLogger(SafGraphqlClient::class.java)

    private val httpClient = HttpClientFactory.create()
    private val azureTokenProvider = AzureAdTokenProvider(
        azureConfig, safConfig.scope
    ).also { LOGGER.info("azure scope: ${safConfig.scope}") }

    companion object {
        private const val JOURNALPOST_PREFIX = "journalpost"
    }

    suspend fun hentJournalpostInfo(journalpostId: Long, callId: String): SafJournalpost {
        if (redis.exists(Key(JOURNALPOST_PREFIX, journalpostId.toString()))) {
            prometheus.cacheHit(JOURNALPOST_PREFIX).increment()
            return redis[Key(JOURNALPOST_PREFIX, journalpostId.toString())]!!.deserialize()
        }
        prometheus.cacheMiss(JOURNALPOST_PREFIX).increment()

        val azureToken = azureTokenProvider.getClientCredentialToken()
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = runBlocking { query(azureToken, request, callId) }

        val journalpost: SafJournalpost =
            response.getOrThrow().data?.journalpost ?: error("Fant ikke journalpost for $journalpostId")
        redis.set(Key(JOURNALPOST_PREFIX, journalpostId.toString()), journalpost.serialize())

        if (journalpost.bruker?.type != BrukerIdType.FNR) {
            log.warn("Journalpost ${journalpostId} har ikke personident")
        }

        return journalpost
    }

    private suspend fun query(accessToken: String, query: SafRequest, callId: String): Result<SafRespons> {
        val request = httpClient.post(safConfig.baseUrl) {
            accept(ContentType.Application.Json)
            header("Nav-Call-Id", callId)
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(query)
        }
        return runCatching {
            val respons = request.body<SafRespons>()
            if (respons.errors != null) {
                log.error("Feil ved henting av journalpost: ${respons.errors}")
                throw SafException("Feil mot SAF: ${respons.errors}")
            }
            respons
        }
    }
}