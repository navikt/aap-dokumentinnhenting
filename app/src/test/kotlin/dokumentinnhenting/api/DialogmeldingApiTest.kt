package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.apiRouting
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.Fakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.repositories.DialogmeldingRepository
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.papsign.ktor.openapigen.model.info.InfoModel
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class DialogmeldingApiTest {

    private lateinit var dataSource: TestDataSource

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `GET eksisterer returnerer 200 OK når dialogmelding finnes`() = testApplication {
        initApp()
        val client = httpClient()

        val uuid = UUID.randomUUID()
        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(lagRecord(uuid))
        }

        val response = client.get("/dialogmelding/$uuid/eksisterer") {
            bearerAuth(bearerToken())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<DialogmeldingEksistererDto>().eksisterer)
    }

    @Test
    fun `GET eksisterer returnerer 204 NoContent når dialogmelding ikke finnes`() = testApplication {
        initApp()
        val client = httpClient()

        val response = client.get("/dialogmelding/${UUID.randomUUID()}/eksisterer") {
            bearerAuth(bearerToken())
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertFalse(response.body<DialogmeldingEksistererDto>().eksisterer)
    }

    @Test
    fun `GET eksisterer returnerer 401 uten gyldig token`() = testApplication {
        initApp()

        val response = client.get("/dialogmelding/${UUID.randomUUID()}/eksisterer")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun bearerToken() =
        AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate()

    private fun lagRecord(uuid: UUID) = DialogmeldingRecord(
        bestillerNavIdent = "Z123456",
        dialogmeldingUuid = uuid,
        behandlerRef = "behandlerRef-123",
        behandlerHprNr = "12344321",
        personIdent = "12345678910",
        personNavn = "Ola Nordmann",
        saksnummer = "SAK-001",
        dokumentasjonType = DokumentasjonType.L8,
        behandlerNavn = "Dr. Behandler",
        fritekst = "En fritekst",
        behandlingsReferanse = UUID.randomUUID(),
    )

    private fun ApplicationTestBuilder.initApp() {
        application {
            commonKtorModule(SimpleMeterRegistry(), AzureConfig(), InfoModel(title = "Test"))
            routing {
                authenticate(AZURE) {
                    apiRouting {
                        dialogmeldingApi(dataSource)
                    }
                }
            }
        }
    }

    private fun ApplicationTestBuilder.httpClient() = createClient {
        install(ContentNegotiation) { jackson() }
    }
}
