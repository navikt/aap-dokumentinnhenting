package dokumentinnhenting.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import dokumentinnhenting.Azp
import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
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
import io.mockk.mockk
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.commonKtorModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.UUID

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
    fun `GET eksisterer returnerer 200 OK når dialogmelding finnes for endepunkt 'eksisterer'`() = testApplication {
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
    fun `GET 'dialogmeldinger' returnerer 200 OK når dialogmelding ikke finnes`() = testApplication {
        initApp()

        val client = httpClient()
        val saksnummer = UUID.randomUUID()

        val response = client.get("/dialogmelding/$saksnummer/dialogmeldinger") {
            bearerAuth(bearerTokenDialogmeldinger())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<List<FellesDialogmeldingDto>>().isEmpty())
    }

    @Test
    fun `GET 'dialogmeldinger' returnerer 200 OK når dialogmelding finnes`() = testApplication {
        initApp()

        val client = httpClient()
        val uuid = UUID.randomUUID()
        val record = lagRecord(uuid)
        val saksnummer = "SAK-001"

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val response = client.get("/dialogmelding/$saksnummer/dialogmeldinger") {
            bearerAuth(bearerTokenDialogmeldinger())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<List<FellesDialogmeldingDto>>().isNotEmpty())
        assertTrue(response.body<List<FellesDialogmeldingDto>>()[0].meldingFraNavn == record.behandlerNavn)
    }

    @Test
    fun `GET 'dialogmeldinger' returnerer riktig antall dialogmeldinger for saksnummer`() = testApplication {
        initApp()

        val client = httpClient()
        val riktigSaksnummer = "SAK-011"
        val annetSaksnummer = "SAK-012"
        val record1 = lagRecord(UUID.randomUUID(), riktigSaksnummer)
        val record2 = lagRecord(UUID.randomUUID(), riktigSaksnummer)
        val record3 = lagRecord(UUID.randomUUID(), annetSaksnummer)
        val mottattRecord1 = lagMottattDialogmelding()
        val mottattRecord2 = lagMottattDialogmelding()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record1)
            DialogmeldingRepository(connection).opprettDialogmelding(record2)
            DialogmeldingRepository(connection).opprettDialogmelding(record3)
            MottattDialogmeldingRepository(connection).lagre(mottattRecord1, riktigSaksnummer)
            MottattDialogmeldingRepository(connection).lagre(mottattRecord2, annetSaksnummer)
        }

        val response = client.get("/dialogmelding/$riktigSaksnummer/dialogmeldinger") {
            bearerAuth(bearerTokenDialogmeldinger())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<List<FellesDialogmeldingDto>>().isNotEmpty())
        assertTrue(response.body<List<FellesDialogmeldingDto>>().count() == 3)
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

    @ParameterizedTest
    @ValueSource(strings = ["eksisterer", "dialogmeldinger"])
    fun `GET eksisterer og dialogmeldinger returnerer 401 uten gyldig token`(endepunkt: String) = testApplication {
        initApp()

        val response = client.get("/dialogmelding/${UUID.randomUUID()}/${endepunkt}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun bearerToken() =
        AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate(
            isApp = true,
            azp = Azp.ApiIntern.toString()
        )

    private fun bearerTokenDialogmeldinger() =
        AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate(
            isApp = true,
            azp = System.getProperty("INTEGRASJON_BEHANDLINGSFLYT_AZP"),
            roles = listOf("dialogmelding-api")
        )

    private fun lagRecord(uuid: UUID, saksnummer: String = "SAK-001") = DialogmeldingRecord(
        bestillerNavIdent = "Z123456",
        dialogmeldingUuid = uuid,
        behandlerRef = "behandlerRef-123",
        behandlerHprNr = "12344321",
        personIdent = "12345678910",
        personNavn = "Ola Nordmann",
        saksnummer = saksnummer,
        dokumentasjonType = DokumentasjonType.L8,
        behandlerNavn = "Dr. Behandler",
        fritekst = "En fritekst",
        behandlingsReferanse = UUID.randomUUID(),
        samtaleRef = UUID.randomUUID(),
    )

    private fun lagMottattDialogmelding(
        msgId: String = UUID.randomUUID().toString(),
        personIdentPasient: String = randomPersonIdent(),
        conversationRef: String? = UUID.randomUUID().toString(),
        parentRef: String? = UUID.randomUUID().toString(),
        legehpr: String? = "12345678",
        tekstNotatInnhold: String? = "tekstNotatInnhold",
        dn: String? = "dn",
        navnHelsepersonell: String = "Dr. Testperson",
        journalpostId: String = "JP-${UUID.randomUUID()}",
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = DialogmeldingMottakDTO(
        msgId = msgId,
        msgType = "DIALOG_NOTAT",
        navLogId = UUID.randomUUID().toString(),
        mottattTidspunkt = mottattTidspunkt,
        conversationRef = conversationRef,
        parentRef = parentRef,
        personIdentPasient = personIdentPasient,
        personIdentBehandler = randomPersonIdent(),
        legekontorOrgNr = "123456789",
        legekontorHerId = "HER-123",
        legekontorOrgName = "Testveien Legekontor AS",
        legehpr = legehpr,
        dialogmelding = Dialogmelding(
            id = UUID.randomUUID().toString(),
            innkallingMoterespons = null,
            foresporselFraSaksbehandlerForesporselSvar = tekstNotatInnhold?.let {
                ForesporselFraSaksbehandlerForesporselSvar(
                    temaKode = TemaKode(
                        kodeverkOID = "kodeverkOID",
                        dn = dn ?: "dn",
                        v = "v",
                        arenaNotatKategori = "arenaNotatKategori",
                        arenaNotatKode = "arenaNotatKode",
                        arenaNotatTittel = "arenaNotatTittel",
                    ),
                    tekstNotatInnhold = tekstNotatInnhold,
                    dokIdNotat = null,
                    datoNotat = null
                )
            },
            henvendelseFraLegeHenvendelse = null,
            navnHelsepersonell = navnHelsepersonell,
            signaturDato = mockk()
        ),
        antallVedlegg = 0,
        journalpostId = journalpostId,
        fellesformatXML = "<xml/>",
    )

    private fun ApplicationTestBuilder.initApp() {
        application {
            commonKtorModule(SimpleMeterRegistry(), InfoModel(title = "Test"), IdentityProvider.ENTRA_ID)
            routing {
                authenticate(IdentityProvider.ENTRA_ID.value) {
                    apiRouting {
                        dialogmeldingApi(dataSource)
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
