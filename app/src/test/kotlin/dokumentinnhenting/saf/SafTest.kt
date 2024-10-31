package dokumentinnhenting.saf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dokumentinnhenting.*
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.HelseDocRequest
import dokumentinnhenting.integrasjoner.saf.Saksnummer
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class SafTest {
    @Test
    fun `henter en journalpost`() {
        Fakes().use { fakes ->
            testApplication {
                val postgres = postgreSQLContainer()
                application { server(TestConfig.default(fakes, DbConfig(postgres.jdbcUrl,postgres.username,postgres.password))) }
                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerModule(JavaTimeModule())
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        }
                    }
                }

                val azureTokenGenerator = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")

                val res = client.post("/saf"){
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

}