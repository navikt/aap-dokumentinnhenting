package dokumentinnhenting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nimbusds.jwt.JWTParser
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway.FinnBehandlingForIdentDTO
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway.NullableSakOgBehandlingDTO
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway.SakOgBehandling
import dokumentinnhenting.integrasjoner.saf.AvsenderMottaker
import dokumentinnhenting.integrasjoner.saf.DokumentInfo
import dokumentinnhenting.integrasjoner.saf.DokumentoversiktFagsak
import dokumentinnhenting.integrasjoner.saf.Dokumentvariant
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.JournalpostSak
import dokumentinnhenting.integrasjoner.saf.Journalposttype
import dokumentinnhenting.integrasjoner.saf.Journalstatus
import dokumentinnhenting.integrasjoner.saf.SafDokumentoversiktFagsakDataResponse
import dokumentinnhenting.integrasjoner.saf.SafDokumentversiktFagsakData
import dokumentinnhenting.integrasjoner.saf.Variantformat
import dokumentinnhenting.integrasjoner.syfo.oppslag.BehandlerOppslagResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import no.nav.aap.brev.kontrakt.HentSignaturDokumentinnhentingRequest
import no.nav.aap.brev.kontrakt.JournalførBehandlerBestillingResponse
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.PersonTilgangRequest
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.random.nextUInt

object Fakes : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val servers = mutableListOf<EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>>()

    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        val azure = embeddedServer(Netty, port = 0, module = { azureFake() }).apply { start() }
        val saf = embeddedServer(Netty, port = 0, module = { safFake() }).apply { start() }
        val syfo = embeddedServer(Netty, port = 0, module = { syfoFake() }).apply { start() }
        val behandlingsflyt = embeddedServer(Netty, port = 0, module = { behandlingsflytFake() }).apply { start() }
        val brev = embeddedServer(Netty, port = 0, module = { brevFake() }).apply { start() }
        val dokarkiv = embeddedServer(Netty, port = 0, module = { dokarkivFake() }).apply { start() }
        val tilgang = embeddedServer(Netty, port = 0, module = { tilgangFake() }).apply { start() }
        val texas = embeddedServer(Netty, port = 0, module = { texasFake() }).apply { start() }

        servers.addAll(
            listOf(
                azure,
                saf,
                syfo,
                behandlingsflyt,
                brev,
                dokarkiv,
                tilgang,
                texas,
            )
        )

        Runtime.getRuntime().addShutdownHook(Thread { close() })

        Thread.currentThread()
            .setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty(
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT",
            "http://localhost:${azure.engine.port()}/token"
        )
        System.setProperty("AZURE_APP_CLIENT_ID", "dokumentinnhenting")
        System.setProperty("AZURE_APP_CLIENT_SECRET", "")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "http://localhost:${azure.engine.port()}/jwks")
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "dokumentinnhenting")
        // saf
        System.setProperty("INTEGRASJON_SAF_URL_REST", "http://localhost:${saf.engine.port()}/rest")
        System.setProperty("INTEGRASJON_SAF_URL_GRAPHQL", "http://localhost:${saf.engine.port()}/graphql")
        System.setProperty("INTEGRASJON_SAF_SCOPE", "saf")

        // Syfo
        System.setProperty("INTEGRASJON_SYFO_BASE_URL", "http://localhost:${syfo.engine.port()}")
        System.setProperty("INTEGRASJON_SYFO_SCOPE", "scope")
        System.setProperty("KAFKA_TRUSTSTORE_PATH", "trust")
        System.setProperty("KAFKA_KEYSTORE_PATH", "store")
        System.setProperty("KAFKA_CREDSTORE_PASSWORD", "password")

        //Behandlingsflyt
        if (System.getenv("INTEGRASJON_BEHANDLINGSFLYT_URL").isNullOrEmpty()) {
            System.setProperty("BEHANDLINGSFLYT_BASE_URL", "http://localhost:${behandlingsflyt.engine.port()}")
        }
        System.setProperty("BEHANDLINGSFLYT_SCOPE", "scope")

        //Brev
        System.setProperty("INTEGRASJON_BREV_BASE_URL", "http://localhost:${brev.engine.port()}")
        System.setProperty("INTEGRASJON_BREV_SCOPE", "http://localhost:${brev.engine.port()}")

        // Dokarkiv
        System.setProperty("INTEGRASJON_DOKARKIV_URL", "http://localhost:${dokarkiv.engine.port()}")
        System.setProperty("INTEGRASJON_DOKARKIV_SCOPE", "http://localhost:${dokarkiv.engine.port()}")

        // Tilgang
        System.setProperty("INTEGRASJON_TILGANG_URL", "http://localhost:${tilgang.engine.port()}")
        System.setProperty("INTEGRASJON_TILGANG_SCOPE", "http://localhost:${tilgang.engine.port()}")

        // Texas
        System.setProperty("NAIS_TOKEN_ENDPOINT", "http://localhost:${texas.engine.port()}/token")
        System.setProperty("NAIS_TOKEN_EXCHANGE_ENDPOINT", "http://localhost:${texas.engine.port()}/token/exchange")
        System.setProperty("NAIS_TOKEN_INTROSPECTION_ENDPOINT", "http://localhost:${texas.engine.port()}/introspect")

        System.setProperty("INTEGRASJON_API_INTERN_AZP", UUID.randomUUID().toString())
        System.setProperty("INTEGRASJON_BEHANDLINGSFLYT_AZP", UUID.randomUUID().toString())

        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        System.setProperty("NAIS_TEAM_AAP", "nais-team-aap")
    }

    override fun close() {
        if (!started.compareAndSet(true, false)) {
            return
        }

        logger.info("Closing Servers.")
        servers.forEach { it.stop(0L, 0L) }
    }

    val behandlingsflytSakResponses: MutableMap<Pair<String, LocalDate>, String> =
        mutableMapOf()

    private fun Application.behandlingsflytFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@behandlingsflytFake.log.info(
                    "BEHANDLINGSFLYT :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        routing {
            post("/api/sak/finnSisteBehandlinger") {
                val body = call.receive<FinnBehandlingForIdentDTO>()
                val key = body.ident to body.mottattTidspunkt
                val sak = behandlingsflytSakResponses[key]?.let { NullableSakOgBehandlingDTO(SakOgBehandling(it)) }
                call.respondNullable(sak)
            }
            post("/api/brev/bestillingvarsel") {
                call.respond(HttpStatusCode.Accepted, "{}")
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

    private fun Application.safFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@safFake.log.info(
                    "AZURE :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
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
                                            DokumentInfo(
                                                dokumentInfoId = "1234",
                                                tittel = "tittel",
                                                brevkode = "kode",
                                                dokumentvarianter = listOf(
                                                    Dokumentvariant(
                                                        variantformat = Variantformat.ARKIV,
                                                        saksbehandlerHarTilgang = true
                                                    )
                                                ),
                                            )
                                        ),
                                        tittel = "tittel",
                                        journalposttype = Journalposttype.I,
                                        temanavn = "aap",
                                        behandlingstemanavn = "aap",
                                        datoOpprettet = LocalDateTime.now().minusDays(1),
                                        relevanteDatoer = null,
                                        journalstatus = Journalstatus.JOURNALFOERT,
                                        tema = "AAP",
                                        behandlingstema = null,
                                        sak = JournalpostSak(
                                            JournalpostSak.Sakstype.FAGSAK,
                                            "AAP",
                                            "KELIVN",
                                            "1A2B3C"
                                        ),
                                        avsenderMottaker = AvsenderMottaker(
                                            "0123456789",
                                            type = AvsenderMottaker.AvsenderMottakerIdType.FNR,
                                            "Test Testesen"
                                        ),
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
                this@syfoFake.log.info(
                    "SYFO :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            get("/api/v1/behandler/personident") {
                call.respond(
                    listOf(
                        behandler("FASTLEGE"),
                        behandler("SYKMELDER")
                    )
                )
            }
            post("/api/v1/behandler/search") {
                call.respond(
                    listOf(
                        behandler("FASTLEGE"),
                        behandler("SYKMELDER")
                    )
                )
            }
        }
    }

    private fun behandler(type: String): BehandlerOppslagResponse {
        return BehandlerOppslagResponse(
            type = type,
            behandlerRef = UUID.randomUUID().toString(),
            kategori = "LE",
            fnr = null,
            hprId = Random.nextInt(1000000, 9999999).toString(),
            fornavn = "fornavn",
            mellomnavn = null,
            etternavn = "etternavn",
            orgnummer = null,
            kontor = null,
            adresse = null,
            postnummer = null,
            poststed = null,
            telefon = null,
        )
    }

    val signaturResponseForRequestNavIdent: MutableMap<String, Signatur?> = mutableMapOf()

    private fun Application.brevFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@brevFake.log.info(
                    "BREV :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/api/dokumentinnhenting/journalfor-behandler-bestilling") {
                call.respond(
                    JournalførBehandlerBestillingResponse(
                        Random.nextUInt().toString(),
                        true,
                        listOf(Random.nextUInt().toString())
                    )
                )
            }
            post("/api/dokumentinnhenting/ekspeder-journalpost-behandler-bestilling") {
                call.respond("")
            }
            post("/api/dokumentinnhenting/forhandsvis-signatur") {
                val request = call.receive<HentSignaturDokumentinnhentingRequest>()
                val response = signaturResponseForRequestNavIdent[request.bestillerNavIdent]
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info(
                    "AZURE :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/token") {
                val body = call.receiveText()
                val token = AzureTokenGen(
                    "dokumentinnhenting",
                    "dokumentinnhenting"
                ).generate(body.contains("grant_type=client_credentials"))
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }

    private fun Application.dokarkivFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@dokarkivFake.log.info(
                    "DOKARKIV :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            put("/rest/journalpostapi/v1/journalpost/{journalpostId}/knyttTilAnnenSak") {
                val journalpostId = call.parameters["journalpostId"]!!
                call.respond(HttpStatusCode.OK, mapOf("nyJournalpostId" to journalpostId))
            }
        }
    }

    private fun Application.tilgangFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@tilgangFake.log.info(
                    "TILGANG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/tilgang/sak") {
                call.receive<SakTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
            post("/tilgang/behandling") {
                call.receive<BehandlingTilgangRequest>()
                call.respond(
                    TilgangResponse(
                        true,
                        tilgangIKontekst = mapOf(Operasjon.SAKSBEHANDLE to true)
                    )
                )
            }
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }

            post("/tilgang/person") {
                call.receive<PersonTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }

    }

    private fun Application.texasFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@texasFake.log.info(
                    "TILGANG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            routing {
                post("/token") {
                    val token = AzureTokenGen("behandlingsflyt", "behandlingsflyt")
                        .generate(isApp = true, azp = "behandlingsflyt")
                    call.respond(TestToken(access_token = token))
                }

                post("/token/exchange") {
                    val body = call.receive<JsonNode>()
                    val NAVident = JWTParser.parse(body["user_token"].asText())
                        .jwtClaimsSet
                        .getClaimAsString("NAVident")

                    val token = AzureTokenGen("behandlingsflyt", body["target"].asText())
                        .generate(isApp = false, azp = "behandlingsflyt", navIdent = NAVident)
                    call.respond(TestToken(access_token = token))
                }

                post("/introspect") {
                    call.respond(mapOf("active" to true))
                }
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