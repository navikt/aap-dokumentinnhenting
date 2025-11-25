package dokumentinnhenting.integrasjoner.brev

import dokumentinnhenting.http.ClientCredentialsTokenProvider
import dokumentinnhenting.http.HttpClientFactory
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytException
import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevGenerering
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.genererBrev
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.brev.kontrakt.HentSignaturDokumentinnhentingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingResponse
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.komponenter.config.requiredConfigForKey
import java.time.LocalDateTime

class BrevClient {
    private val baseUrl = requiredConfigForKey("integrasjon.brev.base.url")
    private val scope = requiredConfigForKey("integrasjon.brev.scope")
    private val httpClient = HttpClientFactory.create()

    fun journalførBestilling(
        bestilling: DialogmeldingFullRecord,
        tidligereBestillingDato: LocalDateTime?
    ): JournalførBehandlerBestillingResponse {
        return runBlocking {
            val token = ClientCredentialsTokenProvider.getToken(scope)
            val body = konstruerBrev(bestilling, tidligereBestillingDato)

            val response = httpClient.post("$baseUrl/api/dokumentinnhenting/journalfor-behandler-bestilling") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                header("Nav-Consumer-Id", "aap-dokumentinnhenting")
                setBody(body)
            }

            if (!response.status.isSuccess()) {
                throw RuntimeException("Failed to journalførBestilling: ${response.status} - ${response.bodyAsText()}")
            }

            response.body<JournalførBehandlerBestillingResponse>()
        }
    }

    fun ekspederBestilling(ekspederRequest: EkspederBestillingRequest) {
        runBlocking {
            try {
                val token = ClientCredentialsTokenProvider.getToken(scope)
                val response = httpClient.post("$baseUrl/api/dokumentinnhenting/ekspeder-journalpost-behandler-bestilling") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    setBody(ekspederRequest)
                }

                if (!response.status.isSuccess()) {
                    throw BehandlingsflytException("Feil ved forsøk på å ekspedere bestilling i brev: ${response.status} - ${response.bodyAsText()}")
                }
            } catch (e: BehandlingsflytException) {
                throw e
            } catch (e: Exception) {
                throw BehandlingsflytException("Feil ved forsøk på å ekspedere bestilling i brev: ${e.message}")
            }
        }
    }

    data class EkspederBestillingRequest(
        val journalpostId: String,
        val dokumentId: String
    )

    private fun konstruerBrev(
        bestilling: DialogmeldingFullRecord,
        tidligereBestillingDato: LocalDateTime?
    ): JournalførBehandlerBestillingRequest {
        val tittel = when (bestilling.dokumentasjonType) {
            DokumentasjonType.L40 -> "Forespørsel om legeerklæring og arbeidsuførhet"
            DokumentasjonType.L8 -> "Forespørsel om tilleggsopplysninger"
            DokumentasjonType.L120 -> "Forespørsel om spesialisterklæring"
            DokumentasjonType.MELDING_FRA_NAV -> "Melding fra NAV"
            DokumentasjonType.RETUR_LEGEERKLÆRING -> "Retur til lege"
            DokumentasjonType.PURRING -> "Purring på forespørsel om legeerklæring arbeidsuførhet"
        }
        val pdfBrevIAvsnitt = mapPdfBrev(bestilling, tidligereBestillingDato)

        val request = JournalførBehandlerBestillingRequest(
            brukerFnr = bestilling.personIdent,
            saksnummer = bestilling.saksnummer,
            mottakerHprnr = bestilling.behandlerHprNr,
            mottakerNavn = bestilling.behandlerNavn,
            eksternReferanseId = bestilling.dialogmeldingUuid,
            brevkode = bestilling.dokumentasjonType.toString(),
            tittel = tittel,
            brevAvsnitt = pdfBrevIAvsnitt,
            dato = bestilling.opprettet.toLocalDate(),
            bestillerNavIdent = bestilling.bestillerNavIdent,
            overstyrInnsynsregel = bestilling.dokumentasjonType.skalVarsleBruker()
        )
        return request
    }

    fun hentSignaturForhåndsvisning(brukerFnr: String, bestillerNavIdent: String): Signatur? {
        return runBlocking {
            val token = ClientCredentialsTokenProvider.getToken(scope)
            val response = httpClient.post("$baseUrl/api/dokumentinnhenting/forhandsvis-signatur") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(
                    HentSignaturDokumentinnhentingRequest(
                        brukerFnr = brukerFnr,
                        bestillerNavIdent = bestillerNavIdent
                    )
                )
            }

            if (!response.status.isSuccess()) {
                return@runBlocking null
            }

            response.body<Signatur?>()
        }
    }

    private fun mapPdfBrev(
        bestilling: DialogmeldingFullRecord,
        tidligereBestillingDato: LocalDateTime?
    ): List<String> {
        val brev = genererBrev(
            BrevGenerering(
                bestilling.personNavn,
                bestilling.personIdent,
                bestilling.fritekst,
                bestilling.dokumentasjonType,
                tidligereBestillingDato
            )
        )
        return brev.split("\n").map { it.replace("""\n""", "") }
    }
}