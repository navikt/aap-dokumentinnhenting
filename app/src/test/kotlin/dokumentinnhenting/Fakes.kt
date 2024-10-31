package dokumentinnhenting

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dokumentinnhenting.integrasjoner.saf.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class Fakes: AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = embeddedServer(Netty, port = 0, module = { azureFake() }).start()
    private val saf = embeddedServer(Netty, port = 0, module = {safFake()}).start()

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azurePort()}/token")
        System.setProperty("azure.app.client.id", "dokumentinnhenting")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azurePort()}/jwks")
        System.setProperty("azure.openid.config.issuer", "dokumentinnhenting")
        // saf
        System.setProperty("integrasjon.saf.url.rest", "http://localhost:${safPort()}/rest")
        System.setProperty("integrasjon.saf.url.graphql", "http://localhost:${safPort()}/graphql")
        System.setProperty("integrasjon.saf.scope", "saf")
        // KAFKA


    }

    fun azurePort(): Int {
        return azure.engine.port()
    }

    fun safPort(): Int {
        return saf.engine.port()
    }

    override fun close() {
        azure.stop(0L, 0L)
        saf.stop(0L,0L)
    }

    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

    private fun Application.safFake(){
        install(ContentNegotiation) {
            jackson{
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@safFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/graphql") {
                call.respond(
                    SafDokumentoversiktFagsakDataResponse(
                        data = SafDokumentversiktFagsakData(
                            DokumentoversiktFagsak(
                                listOf(
                                    Journalpost(
                                        journalpostId = "123",
                                        dokumenter = listOf(
                                            Dokument(
                                                dokumentInfoId = "1234",
                                                tittel = "tittel",
                                                brevkode = "kode",
                                                dokumentvarianter = listOf(
                                                    Dokumentvariant(
                                                        Variantformat.ARKIV
                                                    )
                                                ),
                                            )
                                        ),
                                        tittel = "tittel",
                                        journalposttype = Journalposttype.I,
                                        temanavn = "aap",
                                        behandlingstemanavn = "aap",
                                        datoOpprettet = LocalDateTime.now().minusDays(1),
                                        relevanteDatoer = null
                                    )
                                )
                            )
                        ),
                        errors = null,
                        extensions = null
                    )
                )
            }
        }
    }

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/token") {
                val token = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate()
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }

    internal data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )
}