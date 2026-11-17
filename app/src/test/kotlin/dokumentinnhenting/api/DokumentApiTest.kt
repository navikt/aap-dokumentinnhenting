package dokumentinnhenting.api

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.StatusPagesConfigHelper
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.saf.SafDocumentResponse
import dokumentinnhenting.integrasjoner.saf.SafHentDokumentGateway
import dokumentinnhenting.integrasjoner.saf.Variantformat
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
import io.mockk.coVerify
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
import org.junit.jupiter.params.provider.EnumSource

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
        "application/pdf",
        "application/xml",
        "application/json",
        "image/png",
        "application/octet-stream",
    )
    fun `returnerer dokumentet med innholdstypen fra SAF`(contentType: String) = testApplication {
        initApp()
        val dokument = byteArrayOf(0, 1, 2, 127, -1)
        coEvery {
            SafHentDokumentGateway.hentDokumentMedVariantFormat(
                "journalpostid", "dokumentid", Variantformat.ORIGINAL, any()
            )
        } returns SafDocumentResponse(dokument.inputStream(), contentType, "dokument")

        val response = client.get("/api/dokumenter/journalpostid/dokumentid/ORIGINAL") {
            bearerAuth(bearerToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()).isEqualTo(ContentType.parse(contentType))
        assertThat(response.body<ByteArray>()).isEqualTo(dokument)
    }

    @ParameterizedTest
    @EnumSource(Variantformat::class)
    fun `sender valgt variantformat til SAF`(variantFormat: Variantformat) = testApplication {
        initApp()
        coEvery {
            SafHentDokumentGateway.hentDokumentMedVariantFormat(
                "journalpostid", "dokumentid", variantFormat, any()
            )
        } returns SafDocumentResponse(byteArrayOf(1).inputStream(), "application/pdf", "dokument.pdf")

        val response = client.get("/api/dokumenter/journalpostid/dokumentid/$variantFormat") {
            bearerAuth(bearerToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        coVerify(exactly = 1) {
            SafHentDokumentGateway.hentDokumentMedVariantFormat(
                "journalpostid", "dokumentid", variantFormat, any()
            )
        }
    }

    @Test
    fun `avviser ukjent variantformat`() = testApplication {
        initApp()

        val response = client.get("/api/dokumenter/journalpostid/dokumentid/UKJENT") {
            bearerAuth(bearerToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
        coVerify(exactly = 0) {
            SafHentDokumentGateway.hentDokumentMedVariantFormat(any(), any(), any(), any())
        }
    }

    @Test
    fun `beholder eksisterende PDF-endepunkt`() = testApplication {
        initApp()
        val dokument = "%PDF-1.7".toByteArray()
        coEvery {
            SafHentDokumentGateway.hentDokument("journalpostid", "dokumentid", any())
        } returns SafDocumentResponse(dokument.inputStream(), "application/pdf", "dokument.pdf")

        val response = client.get("/api/dokumenter/journalpostid/dokumentid") {
            bearerAuth(bearerToken())
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()).isEqualTo(ContentType.Application.Pdf)
        assertThat(response.body<ByteArray>()).isEqualTo(dokument)
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
