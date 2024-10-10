package tilgang.integrasjoner.pdl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import tilgang.LOGGER
import tilgang.PdlConfig
import tilgang.auth.AzureAdTokenProvider
import tilgang.auth.AzureConfig
import tilgang.http.HttpClientFactory
import tilgang.metrics.cacheHit
import tilgang.metrics.cacheMiss
import tilgang.redis.Key
import tilgang.redis.Redis
import tilgang.redis.Redis.Companion.deserialize
import tilgang.redis.Redis.Companion.serialize

interface IPdlGraphQLClient {
    suspend fun hentPersonBolk(personidenter: List<String>, callId: String): List<PersonResultat>?

    suspend fun hentGeografiskTilknytning(ident: String, callId: String): HentGeografiskTilknytningResult?
}

class PdlGraphQLClient(
    azureConfig: AzureConfig,
    private val pdlConfig: PdlConfig,
    private val redis: Redis,
    private val prometheus: PrometheusMeterRegistry
) : IPdlGraphQLClient {
    private val httpClient = HttpClientFactory.create()
    private val azureTokenProvider = AzureAdTokenProvider(
        azureConfig,
        pdlConfig.scope
    ).also { LOGGER.info("azure scope: ${pdlConfig.scope}") }

    override suspend fun hentPersonBolk(personidenter: List<String>, callId: String): List<PersonResultat>? {
        val azureToken = azureTokenProvider.getClientCredentialToken()

        val personBolkResult: List<PersonResultat> = personidenter.filter {
            redis.exists(Key(PERSON_BOLK_PREFIX, it))
        }.map {
            redis[Key(PERSON_BOLK_PREFIX, it)]!!.deserialize()
        }
        prometheus.cacheHit(PERSON_BOLK_PREFIX).increment(personBolkResult.size.toDouble())

        val manglendePersonidenter = personidenter.filter {
            !redis.exists(Key(PERSON_BOLK_PREFIX, it))
        }
        
        if (manglendePersonidenter.isEmpty()) {
            return personBolkResult
        }
        
        prometheus.cacheMiss(PERSON_BOLK_PREFIX).increment(manglendePersonidenter.size.toDouble())

        val result = query(azureToken, PdlRequest.hentPersonBolk(manglendePersonidenter), callId)
        val nyePersoner = result.getOrThrow().data?.hentPersonBolk?.map {
            val nyPerson = PersonResultat(
                it.ident,
                it.person?.adressebeskyttelse?.map { it.gradering } ?: emptyList(),
                it.code)
            redis.set(Key(PERSON_BOLK_PREFIX, it.ident), nyPerson.serialize(), 3600)
            nyPerson
        }?.toMutableList()
        nyePersoner?.addAll(personBolkResult)

        return nyePersoner?.toList()
    }

    override suspend fun hentGeografiskTilknytning(ident: String, callId: String): HentGeografiskTilknytningResult {
        val azureToken = azureTokenProvider.getClientCredentialToken()

        if (redis.exists(Key(GEO_PREFIX, ident))) {
            prometheus.cacheHit(GEO_PREFIX).increment()
            return redis[Key(GEO_PREFIX, ident)]!!.deserialize()
        } //TODO: denne kan teknisk sett unngå token credentials, kan vi doppe den, eller burde vi dobbelsjekke
        prometheus.cacheMiss(GEO_PREFIX).increment()

        val result = query(azureToken, PdlRequest.hentGeografiskTilknytning(ident), callId)
        val geoTilknytning = result.getOrThrow().data?.hentGeografiskTilknytning!!
        redis.set(Key(GEO_PREFIX, ident), geoTilknytning.serialize())
        return geoTilknytning
    }

    private suspend fun query(accessToken: String, query: PdlRequest, callId: String): Result<PdlResponse> {
        val request = httpClient.post(pdlConfig.baseUrl) {
            accept(ContentType.Application.Json)
            header("Nav-Call-Id", callId)
            header("TEMA", "AAP")
            header("Behandlingsnummer", BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING)
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(query)
        }
        return runCatching {
            val respons = request.body<PdlResponse>()
            if (respons.errors != null) {
                throw PdlException("Feil mot PDL: ${respons.errors}")
            }
            respons
        }
    }

    companion object {
        private const val PERSON_BOLK_PREFIX = "personBolk"
        private const val GEO_PREFIX = "geografiskTilknytning"
        private const val BEHANDLINGSNUMMER_AAP_SAKSBEHANDLING = "B287"
    }
}

