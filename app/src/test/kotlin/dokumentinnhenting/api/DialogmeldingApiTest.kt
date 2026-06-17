package dokumentinnhenting.api

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import dokumentinnhenting.Azp
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.repositories.DialogmeldingRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DialogmeldingApiTest {

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
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

        assertEquals(HttpStatusCode.OK, response.status)
        assertFalse(response.body<DialogmeldingEksistererDto>().eksisterer)
    }

    @Test
    fun `GET eksisterer returnerer 401 uten gyldig token`() = testApplication {
        initApp()

        val response = client.get("/dialogmelding/${UUID.randomUUID()}/eksisterer")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun bearerToken() =
        AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate(isApp = true, azp = Azp.ApiIntern.toString())

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
