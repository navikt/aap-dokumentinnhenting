package dokumentinnhenting.api

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.StatusPagesConfigHelper
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.saf.SafDocumentResponse
import dokumentinnhenting.integrasjoner.saf.SafHentDokumentGateway
import dokumentinnhenting.integrasjoner.saf.Variantformat
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.commonKtorModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.charset.Charset

@WithFakes
class DokumentApiTest {
    @BeforeEach
    fun setup() {
        mockkObject(SafHentDokumentGateway)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SafHentDokumentGateway)
    }

    @ParameterizedTest
    @CsvSource(
        "application/pdf, UTF-8",
        "application/xml, UTF-8",
        "text/html, UTF-8",
        "text/plain, UTF-8",
        "image/png, UTF-8",
        "application/octet-stream, UTF-8",
        "application/xml; charset=ISO-8859-1, ISO-8859-1",
    )
    fun `returnerer andre innholdstyper som tekst`(contentType: String, charset: String) = testApplication {
        initApp()
        val dokument = "<dokument>Blåbær</dokument>"
        coEvery {
            SafHentDokumentGateway.hentDokument(
                "journalpostid", "dokumentid", any(), Variantformat.ORIGINAL
            )
        } returns SafDocumentResponse(dokument.byteInputStream(Charset.forName(charset)), contentType, "dokument")

        val response = client.get("/api/dokumenter/journalpostid/dokumentid/ORIGINAL") {
            bearerAuth(bearerToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()?.withoutParameters()).isEqualTo(ContentType.Text.Plain)
        assertThat(response.bodyAsText()).isEqualTo(dokument)
    }

    @ParameterizedTest
    @CsvSource(
        """application/json|{"tekst":"Blåbær","antall":2}""",
        """application/json; charset=UTF-8|{"tekst":"Blåbær","antall":2}""",
        "application/json|[]",
        "application/json|\"tekst\"",
        "application/json|42",
        "application/json|true",
        "application/json|null",
        delimiter = '|',
    )
    fun `returnerer JSON uten dobbel serialisering`(contentType: String, json: String) = testApplication {
        initApp()
        coEvery {
            SafHentDokumentGateway.hentDokument(
                "journalpostid", "dokumentid", any(), Variantformat.ORIGINAL
            )
        } returns SafDocumentResponse(json.byteInputStream(), contentType, "dokument.json")

        val response = client.get("/api/dokumenter/journalpostid/dokumentid/ORIGINAL") {
            bearerAuth(bearerToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()?.withoutParameters()).isEqualTo(ContentType.Application.Json)
        assertThat(response.bodyAsText()).isEqualTo(json)
    }

    private fun bearerToken() =
        AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate()

    private fun ApplicationTestBuilder.initApp() {
        application {
            commonKtorModule(SimpleMeterRegistry(), InfoModel(title = "Test"), IdentityProvider.ENTRA_ID)
            install(StatusPages, StatusPagesConfigHelper.setup())
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    apiRouting {
                        dokumentApi(mockk())
                    }
                }
            }
        }
    }
}
