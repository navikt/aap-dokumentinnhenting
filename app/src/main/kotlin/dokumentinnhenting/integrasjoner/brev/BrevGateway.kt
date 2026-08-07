package dokumentinnhenting.integrasjoner.brev

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.SystemTokenProvider
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytException
import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevGenerering
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.genererDialogmelding
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDateTime
import no.nav.aap.brev.kontrakt.HentSignaturDokumentinnhentingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingResponse
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.komponenter.config.requiredConfigForKey

class BrevGateway {
    private val baseUri = requiredConfigForKey("INTEGRASJON_BREV_BASE_URL")
    private val scope = requiredConfigForKey("INTEGRASJON_BREV_SCOPE")

    suspend fun journalførBestilling(
        bestilling: DialogmeldingFullRecord,
        tidligereBestillingDato: LocalDateTime?,
    ): JournalførBehandlerBestillingResponse {
        return defaultHttpClient.post("$baseUri/api/dokumentinnhenting/journalfor-behandler-bestilling") {
            bearerAuth(SystemTokenProvider.getToken(scope, null))
            header("Nav-Consumer-Id", "aap-dokumentinnhenting")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(konstruerBrev(bestilling, tidligereBestillingDato))
        }.body()
    }

    suspend fun ekspederBestilling(ekspederRequest: EkspederBestillingRequest) {
        try {
            defaultHttpClient.post("$baseUri/api/dokumentinnhenting/ekspeder-journalpost-behandler-bestilling") {
                bearerAuth(SystemTokenProvider.getToken(scope, null))
                contentType(ContentType.Application.Json)
                setBody(ekspederRequest)
            }
        } catch (e: Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å ekspedere bestilling i brev: ${e.message}")
        }
    }

    data class EkspederBestillingRequest(
        val journalpostId: String,
        val dokumentId: String,
    )

    private fun konstruerBrev(
        bestilling: DialogmeldingFullRecord,
        tidligereBestillingDato: LocalDateTime?,
    ): JournalførBehandlerBestillingRequest {
        val tittel = when (bestilling.dokumentasjonType) {
            DokumentasjonType.L40 -> "Forespørsel om legeerklæring ved arbeidsuførhet"
            DokumentasjonType.L8 -> "Forespørsel om tilleggsopplysninger"
            DokumentasjonType.L120 -> "Forespørsel om spesialisterklæring"
            DokumentasjonType.MELDING_FRA_NAV -> "Melding fra Nav"
            DokumentasjonType.RETUR_LEGEERKLÆRING -> "Retur til lege"
            DokumentasjonType.PURRING -> "Purring på forespørsel om legeerklæring ved arbeidsuførhet"
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

    suspend fun hentSignaturForhåndsvisning(brukerFnr: String, bestillerNavIdent: String): Signatur? {
        val request = HentSignaturDokumentinnhentingRequest(
            brukerFnr = brukerFnr,
            bestillerNavIdent = bestillerNavIdent
        )
        val response = defaultHttpClient.post("$baseUri/api/dokumentinnhenting/forhandsvis-signatur") {
            bearerAuth(SystemTokenProvider.getToken(scope, null))
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    private fun mapPdfBrev(
        bestilling: DialogmeldingFullRecord,
        tidligereBestillingDato: LocalDateTime?,
    ): List<String> {
        return genererDialogmelding(
            BrevGenerering(
                personNavn = bestilling.personNavn,
                personIdent = bestilling.personIdent,
                dialogmeldingTekst = bestilling.fritekst,
                dokumentasjonType = bestilling.dokumentasjonType,
                tidligereBestillingDato = tidligereBestillingDato,
                signatur = null // Signatur er i PDF-mal
            )
        ).split("\n\n")
    }
}