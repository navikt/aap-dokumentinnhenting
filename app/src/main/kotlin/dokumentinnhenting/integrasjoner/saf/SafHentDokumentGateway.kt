package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.InputStream
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object SafHentDokumentGateway {
    private val logger: Logger = LoggerFactory.getLogger(SafHentDokumentGateway::class.java)

    private val safBaseUrl = requiredConfigForKey("integrasjon.saf.url.rest")
    private val scope = requiredConfigForKey("integrasjon.saf.scope")

    suspend fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        currentToken: OidcToken,
    ): SafDocumentResponse {
        // Se https://confluence.adeo.no/display/BOA/Enum%3A+Variantformat
        // for gyldige verdier

        val res = try {
            defaultHttpClient.get("$safBaseUrl/hentdokument/${journalpostId}/${dokumentInfoId}/ARKIV") {
                bearerAuth(OboTokenProvider.getToken(scope, currentToken))
            }
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Forbidden -> {
                    throw IkkeTillattException("Mangler tilgang til å se dokument i SAF")
                }
                HttpStatusCode.NotFound -> {
                    logger.warn("Fant ikke dokument i SAF. JournalpostId: $journalpostId, DokumentInfoId: $dokumentInfoId")
                    throw VerdiIkkeFunnetException("Dokument ikke funnet i Joark.")
                }
                else -> throw e
            }
        }

        val headers = res.headers
        val contentType = res.contentType().toString()
        val filnavn: String = extractFileNameFromHeaders(headers)
            ?: throw IllegalStateException("Respons inneholdt ikke korrekte headere: $headers")

        return SafDocumentResponse(dokument = res.body(), contentType = contentType, filnavn = filnavn)
    }

    private fun extractFileNameFromHeaders(headers: Headers): String? {
        val value = headers["Content-Disposition"]
        if (value.isNullOrBlank()) {
            return null
        }
        return Regex("filename=([^;]+)")
            .find(value)
            ?.groupValues
            ?.get(1)
    }
}

data class SafDocumentResponse(val dokument: InputStream, val contentType: String, val filnavn: String)
