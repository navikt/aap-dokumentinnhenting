package dokumentinnhenting

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.saf.*
import dokumentinnhenting.integrasjoner.syfo.oppslag.BehandlerOppslagResponse
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
import java.util.*

class Fakes: AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = embeddedServer(Netty, port = 0, module = { azureFake() }).start()
    private val saf = embeddedServer(Netty, port = 0, module = {safFake()}).start()
    private val syfo = embeddedServer(Netty, port = 0, module = {syfoFake()}).start()
    private val behandlingsflyt = embeddedServer(Netty, port = 0, module = {behandlingsflytFake()}).start()
    private val brev = embeddedServer(Netty, port = 0, module = {brevFake()}).start()

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

        // Syfo
        System.setProperty("integrasjon.syfo.base.url", "http://localhost:${syfoPort()}")
        System.setProperty("integrasjon.syfo.scope", "scope")
        System.setProperty("kafka.truststore.path", "trust")
        System.setProperty("kafka.keystore.path", "store")
        System.setProperty("kafka.credstore.password", "password")

        //Behandlingsflyt
        System.setProperty("behandlingsflyt.base.url", "http://localhost:${behandlingsflytPort()}")
        System.setProperty("behandlingsflyt.scope", "scope")

        //Brev
        System.setProperty("integrasjon.brev.base.url", "http://localhost:${brevPort()}")
        System.setProperty("integrasjon.brev.scope", "http://localhost:${brevPort()}")

        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    fun azurePort(): Int {
        return azure.engine.port()
    }

    fun safPort(): Int {
        return saf.engine.port()
    }

    fun syfoPort(): Int {
        return syfo.engine.port()
    }

    fun behandlingsflytPort(): Int {
        return behandlingsflyt.engine.port()
    }

    fun brevPort(): Int {
        return brev.engine.port()
    }

    override fun close() {
        azure.stop(0L, 0L)
        saf.stop(0L,0L)
        syfo.stop(0, 0L)
        brev.stop(0, 0L)
        behandlingsflyt.stop(0, 0L)
    }

    private fun Application.behandlingsflytFake(){
        install(ContentNegotiation) {
            jackson{
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@behandlingsflytFake.log.info("BEHANDLINGSFLYT :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }

        routing {
            post("/api/sak/finnSisteBehandlinger") {
                call.respond(
                    BehandlingsflytClient.NullableSakOgBehandlingDTO(
                        BehandlingsflytClient.SakOgBehandling(
                            personIdent = "personIdentPasient",
                            saksnummer = "saksnummer",
                            "",
                            sisteBehandlingStatus = ""
                        )
                    )
                )
            }
            post("/api/hendelse/send") {
                call.respond {}
            }
        }
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
            get("/rest/hentdokument/journalpostid/dokumentid/${Variantformat.ARKIV}") {
                call.respond("")
            }
        }
    }

    private fun Application.syfoFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@syfoFake.log.info("SYFO :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing{
            route("/syfo") {
                get("/behandleroppslag/search") {
                    call.respond(listOf(
                        BehandlerOppslagResponse(
                            "type1", UUID.randomUUID().toString(), "32341234123", "Peppa", "The", "Pig", "33333", "Fløyen Kontor", "Bergensveien", "5221", "Bergen", "11223344", "hprId"
                        ),
                        BehandlerOppslagResponse(
                            "type2", UUID.randomUUID().toString(), "22341234123", "Ola", "Brunost", "Fårepølse", "33333", "Fløyen Kontor", "Bergensveien", "5221", "Bergen", "44332211", "hprId"
                        ),
                        BehandlerOppslagResponse(
                            "type3", UUID.randomUUID().toString(), "12341234123", "Kari", "", "Tau", "33333", "Fløyen Kontor", "Bergensveien", "5221", "Bergen", "22113344", "hrpId"
                        ),
                    ))
                }
            }
        }
    }

    private fun Application.brevFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@brevFake.log.info("BREV :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/api/dokumentinnhenting/ekspeder-journalpost-behandler-bestilling") {
                call.respond("")
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