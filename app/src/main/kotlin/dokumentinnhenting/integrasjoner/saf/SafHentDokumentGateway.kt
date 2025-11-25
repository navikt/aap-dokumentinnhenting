package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.http.HttpClientFactory
import dokumentinnhenting.http.OnBehalfOfTokenProvider
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import org.slf4j.LoggerFactory
import java.io.InputStream

private val log = LoggerFactory.getLogger(SafHentDokumentGateway::class.java)

class SafHentDokumentGateway {
    private val restUrl = requiredConfigForKey("integrasjon.saf.url.rest")
    private val scope = requiredConfigForKey("integrasjon.saf.scope")
    private val httpClient = HttpClientFactory.create()

    companion object {
        fun extractFileNameFromHeaders(headers: io.ktor.http.Headers): String? {
            val value = headers["Content-Disposition"]
            if (value.isNullOrBlank()) {
                return null
            }
            val regex = Regex("filename=([^;]+)")
            val matchResult = regex.find(value)
            return matchResult?.groupValues?.get(1)
        }

        fun withDefaultRestClient(): SafHentDokumentGateway {
            return SafHentDokumentGateway()
        }
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        currentToken: String
    ): SafDocumentResponse {
        return runBlocking {
            val safURI = "$restUrl/hentdokument/$journalpostId/$dokumentInfoId/ARKIV"
            log.info("Kaller SAF med URL: $safURI.")

            val token = OnBehalfOfTokenProvider.getToken(scope, currentToken)
            val response = httpClient.get(safURI) {
                bearerAuth(token)
            }

            if (!response.status.isSuccess()) {
                throw RuntimeException("Failed to fetch document from SAF: ${response.status} - ${response.bodyAsText()}")
            }

            val contentType = response.headers["Content-Type"]
            val filnavn = extractFileNameFromHeaders(response.headers)

            if (contentType == null || filnavn == null) {
                throw IllegalStateException("Respons inneholdt ikke korrekte headere: ${response.headers}")
            }

            SafDocumentResponse(
                dokument = response.bodyAsChannel().toInputStream(),
                contentType = contentType,
                filnavn = filnavn
            )
        }
    }
}

data class SafDocumentResponse(val dokument: InputStream, val contentType: String, val filnavn: String)