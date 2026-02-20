package dokumentinnhenting.integrasjoner.behandlingsflyt

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.SystemTokenProvider
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.komponenter.config.requiredConfigForKey

object BehandlingsflytGateway {
    private val uri = requiredConfigForKey("behandlingsflyt.base.url")
    private val scope = requiredConfigForKey("behandlingsflyt.scope")

    fun taSakAvVent(taAvVentRequest: Innsending) = runBlocking {
        try {
            defaultHttpClient.post("$uri/api/hendelse/send") {
                bearerAuth(SystemTokenProvider.getToken(scope, null))
                contentType(ContentType.Application.Json)
                setBody(taAvVentRequest)
            }
        } catch (e: Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å ta sak av vent i behandlingsflyt: ${e.message}")
        }
    }

    fun sendVarslingsbrev(varselRequest: VarselOmBrevbestillingDto) = runBlocking {
        try {
            defaultHttpClient.post("$uri/api/brev/bestillingvarsel") {
                bearerAuth(SystemTokenProvider.getToken(scope, null))
                contentType(ContentType.Application.Json)
                setBody(varselRequest)
            }
        } catch (e: Exception) {
            throw BehandlingsflytException("Feilet ved bestilling av varslingsbrev: ${e.message}")
        }
    }

    fun finnÅpenSakForIdentPåDato(
        personIdentPasient: String,
        toLocalDate: LocalDate,
    ): SakOgBehandling? = runBlocking {
        try {
            defaultHttpClient.post("$uri/api/sak/finnSisteBehandlinger") {
                bearerAuth(SystemTokenProvider.getToken(scope, null))
                contentType(ContentType.Application.Json)
                setBody(FinnBehandlingForIdentDTO(personIdentPasient, toLocalDate))
            }.body<NullableSakOgBehandlingDTO?>()?.sakOgBehandlingDTO
        } catch (e: Exception) {
            throw BehandlingsflytException("Feilet ved henting av sak/behandling for ident: ${e.message}")
        }
    }

    data class SakOgBehandling(
        val saksnummer: String,
    )

    private data class FinnBehandlingForIdentDTO(
        val ident: String,
        val mottattTidspunkt: LocalDate,
    )

    private data class NullableSakOgBehandlingDTO(
        val sakOgBehandlingDTO: SakOgBehandling?,
    )
}
