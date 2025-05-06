package dokumentinnhenting.saf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.DbConfig
import dokumentinnhenting.Fakes
import dokumentinnhenting.TestConfig
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.HelseDocRequest
import dokumentinnhenting.integrasjoner.saf.Saksnummer
import dokumentinnhenting.postgreSQLContainer
import dokumentinnhenting.server
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import java.util.UUID
import kotlin.test.assertEquals

class SafTest {
    //@Test
    fun `henter en journalpost`() {
        val fakes = Fakes
        testApplication {
            val postgres = postgreSQLContainer()
            application {
                server(
                    TestConfig.default(
                        fakes,
                        DbConfig(postgres.jdbcUrl, postgres.username, postgres.password)
                    )
                )
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    }
                }
            }

            val azureTokenGenerator = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")

            val res = client.post("/saf") {
                bearerAuth(azureTokenGenerator.generate())
                header("Nav-CallId", UUID.randomUUID())
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(HelseDocRequest(Saksnummer("123456789")))
            }

            val body = res.body<List<Doc>>()

            assertEquals(1, body.size)
        }
    }
}