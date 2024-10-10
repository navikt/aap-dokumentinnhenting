package tilgang.integrasjoner.skjerming

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import tilgang.LOGGER
import tilgang.SkjermingConfig
import tilgang.auth.AzureAdTokenProvider
import tilgang.auth.AzureConfig
import tilgang.http.HttpClientFactory
import tilgang.integrasjoner.behandlingsflyt.IdenterRespons
import tilgang.metrics.cacheHit
import tilgang.metrics.cacheMiss
import tilgang.redis.Key
import tilgang.redis.Redis
import tilgang.redis.Redis.Companion.deserialize
import tilgang.redis.Redis.Companion.serialize


open class SkjermingClient(
    azureConfig: AzureConfig,
    private val skjermingConfig: SkjermingConfig,
    private val redis: Redis,
    private val prometheus: PrometheusMeterRegistry
) {
    private val azureTokenProvider = AzureAdTokenProvider(
        azureConfig,
        skjermingConfig.scope
    ).also { LOGGER.info("azure scope: ${skjermingConfig.scope}") }
    val httpClient = HttpClientFactory.create()

    open suspend fun isSkjermet(identer: IdenterRespons): Boolean {
        val azureToken = azureTokenProvider.getClientCredentialToken()
        
        if (redis.exists(Key(SKJERMING_PREFIX, identer.søker.first()))) {
            prometheus.cacheHit(SKJERMING_PREFIX).increment()
            return redis[Key(SKJERMING_PREFIX, identer.søker.first())]!!.deserialize()
        }
        prometheus.cacheMiss(SKJERMING_PREFIX).increment()

        val url = "${skjermingConfig.baseUrl}/skjermetBulk"
        val alleRelaterteSøkerIdenter = identer.søker + identer.barn
        
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            bearerAuth(azureToken)
            setBody(SkjermetDataBulkRequestDTO(alleRelaterteSøkerIdenter))
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                val result = response.body<Map<String, Boolean>>()
                val eksistererSkjermet = result.values.any { identIsSkjermet -> identIsSkjermet }
                redis.set(Key(SKJERMING_PREFIX, identer.søker.first()), eksistererSkjermet.serialize())
                eksistererSkjermet
            }

            else -> throw SkjermingException("Feil ved henting av skjerming for ident: ${response.status} : ${response.bodyAsText()}")
        }

    }

    companion object {
        private const val SKJERMING_PREFIX = "skjerming"
    }

}

internal data class SkjermetDataBulkRequestDTO(val personidenter: List<String>)