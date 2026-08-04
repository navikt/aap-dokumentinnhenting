package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.azure.SystemTokenProvider
import dokumentinnhenting.integrasjoner.dokarkiv.DokarkivGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HåndterMottattDialogmeldingUtførerTest {

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
    fun `utfør oppretter jobb for TaSakAvVentUtfører`() {
        val payload = lagPayload()

        utfør(payload)

        val jobber = hentJobber(payload.dialogmeldingMottatt.personIdentPasient)
        assertEquals(1, jobber.size)
    }

    @Test
    fun `utfør lagrer dialogmelding når skalLagreMottattDialogmelding er true`() {
        val payload = lagPayload(skalLagre = true)
        val msgId = UUID.fromString(payload.dialogmeldingMottatt.msgId)

        utfør(payload)

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }

        assertNotNull(lagret)
        assertEquals(msgId, lagret!!.msgId)
        assertEquals(payload.sakOgBehandling.saksnummer, lagret.saksnummer)
        assertEquals(payload.dialogmeldingMottatt.journalpostId, lagret.journalpostId)
        assertEquals(payload.dialogmeldingMottatt.legehpr, lagret.legehpr)
    }

    @Test
    fun `utfør lagrer ikke dialogmelding når skalLagreMottattDialogmelding er false`() {
        val payload = lagPayload(skalLagre = false)
        val msgId = UUID.fromString(payload.dialogmeldingMottatt.msgId)

        utfør(payload)

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }

        assertNull(lagret)
    }

    private fun utfør(payload: FiltrertDialogmeldingMedSakstilknytning) {
        dataSource.transaction { connection ->
            HåndterMottattDialogmeldingUtfører(
                DokarkivGateway(SystemTokenProvider),
                FlytJobbRepository(connection),
                MottattDialogmeldingRepository(connection),
            ).utfør(JobbInput(HåndterMottattDialogmeldingUtfører).medPayload(DefaultJsonMapper.toJson(payload)))
        }
    }

    private fun hentJobber(innhold: String): List<String> =
        dataSource.transaction { connection ->
            connection.queryList("SELECT PAYLOAD FROM JOBB WHERE TYPE = ? AND PAYLOAD LIKE ?") {
                setParams {
                    setString(1, "taSakAvVent.handler")
                    setString(2, "%$innhold%")
                }
                setRowMapper { it.getString("PAYLOAD") }
            }
        }

    private fun lagPayload(
        skalLagre: Boolean = true,
    ) = FiltrertDialogmeldingMedSakstilknytning(
        skalLagreMottattDialogmelding = skalLagre,
        dialogmeldingMottatt = DialogmeldingMottakDTO(
            msgId = UUID.randomUUID().toString(),
            msgType = "DIALOG_NOTAT",
            navLogId = UUID.randomUUID().toString(),
            mottattTidspunkt = LocalDateTime.now(),
            conversationRef = UUID.randomUUID().toString(),
            parentRef = null,
            personIdentPasient = randomPersonIdent(),
            personIdentBehandler = randomPersonIdent(),
            legekontorOrgNr = "123456789",
            legekontorHerId = "HER-123",
            legekontorOrgName = "Testlegekontor",
            legehpr = Random.nextInt().toString(),
            dialogmelding = Dialogmelding(
                id = UUID.randomUUID().toString(),
                innkallingMoterespons = null,
                foresporselFraSaksbehandlerForesporselSvar = ForesporselFraSaksbehandlerForesporselSvar(
                    temaKode = TemaKode(
                        kodeverkOID = "2.16.578.1.12.4.1.1.8127",
                        dn = "Svar på forespørsel",
                        v = "DIALOGSVAR",
                        arenaNotatKategori = "ES",
                        arenaNotatKode = "DIALOG",
                        arenaNotatTittel = "Dialogsvar"
                    ),
                    tekstNotatInnhold = "Svar fra lege",
                    dokIdNotat = null,
                    datoNotat = null,
                ),
                henvendelseFraLegeHenvendelse = null,
                navnHelsepersonell = "Dr. Testesen",
                signaturDato = LocalDateTime.now(),
            ),
            antallVedlegg = 0,
            journalpostId = Random.nextLong().toString(),
            fellesformatXML = "<xml/>",
        ),
        sakOgBehandling = BehandlingsflytGateway.SakOgBehandling(saksnummer = "ABC123"),
    )
}
