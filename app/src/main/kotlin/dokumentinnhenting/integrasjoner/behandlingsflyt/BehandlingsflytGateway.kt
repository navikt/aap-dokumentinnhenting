package dokumentinnhenting.integrasjoner.behandlingsflyt

import dokumentinnhenting.util.metrics.prometheus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI
import java.time.LocalDate
import java.util.*

object BehandlingsflytGateway {
    private val uri = requiredConfigForKey("behandlingsflyt.base.url")
    private val config = ClientConfig(scope = requiredConfigForKey("behandlingsflyt.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    fun taSakAvVent(taAvVentRequest: Innsending) {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = taAvVentRequest
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create("$uri/api/hendelse/send"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å ta sak av vent i behandlingsflyt: ${e.message}")
        }
    }

    fun sendVarslingsbrev(varselRequest: VarselOmBrevbestillingDto) {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = varselRequest
        )

        try {
            val uri = URI.create("$uri/api/brev/bestillingvarsel")
            client.post<_, Unit>(uri, request)
        } catch (e: Exception) {
            throw BehandlingsflytException("Feilet ved bestilling av varslingsbrev: ${e.message}")
        }
    }

    fun finnÅpenSakForIdentPåDato(personIdentPasient: String, toLocalDate: LocalDate): NullableSakOgBehandlingDTO? {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = FinnBehandlingForIdentDTO(personIdentPasient, toLocalDate),
        )

        try {
            return client.post(
                    uri = URI.create(uri).resolve("/api/sak/finnSisteBehandlinger"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body)})
        } catch (e: Exception) {
            throw BehandlingsflytException("Feilet ved henting av sak/behandling for ident: ${e.message}")
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
