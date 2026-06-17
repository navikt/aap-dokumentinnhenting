package dokumentinnhenting.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.StatusPagesConfigHelper
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.dokumentinnhenting.kontrakt.FastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentFastlegeDto
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyfoApiTest {

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
    fun `POST behandleroppslag fastlege returnerer 200 OK med FastlegeDto gitt bruk av HentFastlegeDto i kontrakt`() =
        testApplication {
            initApp()
            val client = httpClient()

            val response = client.post("/syfo/behandleroppslag/fastlege") {
                bearerAuth(bearerToken())
                contentType(ContentType.Application.Json)
                setBody(HentFastlegeDto(saksnummer = "SAK-123", personIdent = "12345678910"))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val dto = response.body<FastlegeDto>()
            assertNotNull(dto.fastlege)
        }

    private fun bearerToken() =
        AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate(isApp = false)

    private fun ApplicationTestBuilder.initApp() {
        application {
            commonKtorModule(SimpleMeterRegistry(), AzureConfig(), InfoModel(title = "Test"))
            install(StatusPages, StatusPagesConfigHelper.setup())
            routing {
                authenticate(AZURE) {
                    apiRouting {
                        syfoApi(dataSource, BrevGateway(), SyfoGateway())
                    }
                }
            }
        }
    }

    private fun ApplicationTestBuilder.httpClient() = createClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }
}
