package dokumentinnhenting.integrasjoner.behandlingsflyt

import dokumentinnhenting.http.ClientCredentialsTokenProvider
import dokumentinnhenting.http.HttpClientFactory
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.komponenter.config.requiredConfigForKey
import java.time.LocalDate

object BehandlingsflytClient {
    private val baseUrl = requiredConfigForKey("behandlingsflyt.base.url")
    private val scope = requiredConfigForKey("behandlingsflyt.scope")

    private val httpClient = HttpClientFactory.create()

    fun taSakAvVent(taAvVentRequest: Innsending) {
        runBlocking {
            try {
                val token = ClientCredentialsTokenProvider.getToken(scope)
                val response = httpClient.post("$baseUrl/api/hendelse/send") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(taAvVentRequest)
                }

                if (!response.status.isSuccess()) {
                    throw BehandlingsflytException("Feil ved forsøk på å ta sak av vent i behandlingsflyt: ${response.status} - ${response.bodyAsText()}")
                }
            } catch (e: BehandlingsflytException) {
                throw e
            } catch (e: Exception) {
                throw BehandlingsflytException("Feil ved forsøk på å ta sak av vent i behandlingsflyt: ${e.message}")
            }
        }
    }

    fun sendVarslingsbrev(varselRequest: VarselOmBrevbestillingDto) {
        runBlocking {
            try {
                val token = ClientCredentialsTokenProvider.getToken(scope)
                val response = httpClient.post("$baseUrl/api/brev/bestillingvarsel") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(varselRequest)
                }

                if (!response.status.isSuccess()) {
                    throw BehandlingsflytException("Feilet ved bestilling av varslingsbrev: ${response.status} - ${response.bodyAsText()}")
                }
            } catch (e: BehandlingsflytException) {
                throw e
            } catch (e: Exception) {
                throw BehandlingsflytException("Feilet ved bestilling av varslingsbrev: ${e.message}")
            }
        }
    }

    fun finnÅpenSakForIdentPåDato(personIdentPasient: String, toLocalDate: LocalDate): NullableSakOgBehandlingDTO? {
        return runBlocking {
            try {
                val token = ClientCredentialsTokenProvider.getToken(scope)
                val response = httpClient.post("$baseUrl/api/sak/finnSisteBehandlinger") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(FinnBehandlingForIdentDTO(personIdentPasient, toLocalDate))
                }

                if (!response.status.isSuccess()) {
                    throw BehandlingsflytException("Feilet ved henting av sak/behandling for ident: ${response.status} - ${response.bodyAsText()}")
                }

                response.body<NullableSakOgBehandlingDTO>()
            } catch (e: BehandlingsflytException) {
                throw e
            } catch (e: Exception) {
                throw BehandlingsflytException("Feilet ved henting av sak/behandling for ident: ${e.message}")
            }
        }
    }

    data class SakOgBehandling(
        val personIdent: String,
        val saksnummer: String,
        val status: String,
        val sisteBehandlingStatus: String
    )

    data class FinnBehandlingForIdentDTO(
        val ident: String,
        val mottattTidspunkt: LocalDate
    )

    data class NullableSakOgBehandlingDTO(
        val sakOgBehandlingDTO: SakOgBehandling?
    )
}
