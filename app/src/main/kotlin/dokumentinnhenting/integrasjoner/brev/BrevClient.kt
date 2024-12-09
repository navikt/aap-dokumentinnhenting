package dokumentinnhenting.integrasjoner.brev

import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevGenerering
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.genererBrev
import no.nav.aap.brev.kontrakt.*
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI
import java.time.LocalDateTime

class BrevClient {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev.base.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.brev.scope"))
    private val client = RestClient.withDefaultResponseHandler(config = config, tokenProvider = ClientCredentialsTokenProvider)

    fun journalførBestilling(bestilling: DialogmeldingFullRecord, tidligereBestillingDato: LocalDateTime? ): JournalførBehandlerBestillingResponse {
        val uri = baseUri.resolve("/api/dokumentinnhenting/journalfor-behandler-bestilling")
        val body = konstruerBrev(bestilling, tidligereBestillingDato)
        val httpRequest = PostRequest(
            body = body,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-dokumentinnhenting"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri, httpRequest))
    }

    private fun konstruerBrev(bestilling: DialogmeldingFullRecord, tidligereBestillingDato: LocalDateTime?): JournalførBehandlerBestillingRequest {
        val tittel = when(bestilling.dokumentasjonType) {
            DokumentasjonType.L40 -> "Forespørsel om legeerklæring og arbeidsuførhet"
            DokumentasjonType.L8 -> "Forespørsel om tilleggsopplysninger"
            DokumentasjonType.L120 -> "Forespørsel om spesialisterklæring"
            DokumentasjonType.MELDING_FRA_NAV -> "Melding fra NAV"
            DokumentasjonType.RETUR_LEGEERKLÆRING -> "Retur legeerklæring"
            DokumentasjonType.PURRING -> "Purring på forespørsel om legeerklæring"
        }
        val pdfBrevIAvsnitt = mapPdfBrev(bestilling, tidligereBestillingDato)

        val request = JournalførBehandlerBestillingRequest(
            bestilling.personIdent,
            bestilling.saksnummer,
            bestilling.behandlerHprNr,
            bestilling.behandlerNavn,
            bestilling.dialogmeldingUuid,
            bestilling.dokumentasjonType.toString(),
            tittel,
            pdfBrevIAvsnitt,
            bestilling.opprettet.toLocalDate()
        )
        return request
    }

    //Todo: Flytte hele greien til sanity så vi faktisk får noe fornuftig stuktur? Brev skal også refaktorere bruken av denne
    private fun mapPdfBrev(bestilling: DialogmeldingFullRecord, tidligereBestillingDato: LocalDateTime?): List<String> {
        val brev = genererBrev(
            BrevGenerering(
                bestilling.personNavn, bestilling.personIdent, bestilling.fritekst, bestilling.veilederNavn, bestilling.dokumentasjonType, tidligereBestillingDato
            )
        )
        return brev.split("\n").map{it.replace("""\n""", "")}
    }
}