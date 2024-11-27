package dokumentinnhenting.integrasjoner.brev

import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevGenerering
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.genererBrev
import no.nav.aap.brev.kontrakt.*
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.brev.kontrakt.PdfBrev.*
import no.nav.aap.brev.kontrakt.PdfBrev.Blokk
import no.nav.aap.brev.kontrakt.PdfBrev.Innhold
import no.nav.aap.brev.kontrakt.PdfBrev.Tekstbolk
import java.net.URI
import java.time.LocalDateTime

class BrevClient {
    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.brev.scope"))
    private val client = RestClient.withDefaultResponseHandler(config = config, tokenProvider = ClientCredentialsTokenProvider)

    fun journalførBestilling(bestilling: DialogmeldingFullRecord, tidlereBestillingDato: LocalDateTime? ): JournalpostIdResponse {
       /* val uri = baseUri.resolve("/api/journalforbrev")
        val body = konstruerBrev(bestilling, tidlereBestillingDato)
        val httpRequest = PostRequest(
            body = body,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-dokumentinnhenting"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri, httpRequest))*/
        return JournalpostIdResponse("")
    }

    /*
    private fun konstruerBrev(bestilling: DialogmeldingFullRecord, tidlereBestillingDato: LocalDateTime?): JournalførBrevRequest {
        val pdfBrev = mapPdfBrev(bestilling, tidlereBestillingDato)
        val request = JournalførBrevRequest(
            "FNR",
            "NAVN",
            bestilling.saksnummer,
            bestilling.dialogmeldingUuid,
            "tittel",
            bestilling.dokumentasjonType.toString(),
            pdfBrev
        )
        return request
    }
    */

/*
    private fun mapPdfBrev(bestilling: DialogmeldingFullRecord, tidlereBestillingDato: LocalDateTime?): PdfBrev {

        val brev = genererBrev(
            BrevGenerering(
                bestilling.personNavn, bestilling.personIdent, bestilling.fritekst, bestilling.veilederNavn, bestilling.dokumentasjonType, tidlereBestillingDato
            )
        )

        val brevIAvsnitt = brev.split("\n")

        return PdfBrev(
            mottaker = Mottaker(navn = personinfo.navn, ident = personinfo.fnr),
            saksnummer = saksnummer.nummer,
            dato = dato,
            overskrift = brev.overskrift,
            tekstbolker = brev.tekstbolker.map { // TODO: her må vi splitte de selv...Generer tekst, split
                Tekstbolk(
                    overskrift = it.overskrift,
                    innhold = it.innhold.map {
                        Innhold(
                            overskrift = it.overskrift,
                            blokker = it.blokker.map {
                                Blokk(
                                    innhold = it.innhold.mapNotNull {
                                        when (it) {
                                            is BlokkInnhold.FormattertTekst -> FormattertTekst(
                                                tekst = it.tekst,
                                                formattering = it.formattering
                                            )

                                            else -> null
                                        }
                                    },
                                    type = it.type
                                )
                            })
                    })
            },
        )
    }*/
}