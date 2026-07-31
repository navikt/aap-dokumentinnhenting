package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.DialogmeldingMedSakstilknytning
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.randomPersonIdent
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MottattDialogmeldingRepositoryTest {

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
    fun `lagre lagrer dialogmelding og hentForMsgId returnerer den`() {
        val payload = lagPayload()
        val msgId = UUID.fromString(payload.dialogmeldingMottatt.msgId)

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(payload)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }

        assertNotNull(lagret)
        assertEquals(msgId, lagret!!.msgId)
        assertEquals(payload.dialogmeldingMottatt.msgType, lagret.msgType)
        assertEquals(payload.dialogmeldingMottatt.personIdentPasient, lagret.personIdentPasient)
        assertEquals(payload.dialogmeldingMottatt.journalpostId, lagret.journalpostId)
        assertEquals(payload.dialogmeldingMottatt.legehpr, lagret.legehpr)
        assertEquals(payload.sakOgBehandling.saksnummer, lagret.saksnummer)
    }

    @Test
    fun `lagre lagrer conversationRef og parentRef korrekt`() {
        val conversationRef = UUID.randomUUID()
        val parentRef = UUID.randomUUID()
        val payload = lagPayload(
            conversationRef = conversationRef.toString(),
            parentRef = parentRef.toString(),
        )

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(payload)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(UUID.fromString(payload.dialogmeldingMottatt.msgId))
        }!!

        assertEquals(conversationRef, lagret.conversationRef)
        assertEquals(parentRef, lagret.parentRef)
    }

    @Test
    fun `lagre håndterer null-felter for conversationRef, parentRef og legehpr`() {
        val payload = lagPayload(
            conversationRef = null,
            parentRef = null,
            legehpr = null,
        )

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(payload)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(UUID.fromString(payload.dialogmeldingMottatt.msgId))
        }!!

        assertNull(lagret.conversationRef)
        assertNull(lagret.parentRef)
        assertNull(lagret.legehpr)
    }

    @Test
    fun `duplikat med samme msgId kaster exception`() {
        val msgId = UUID.randomUUID()
        val payload = lagPayload(msgId = msgId.toString(), saksnummer = "SAK-ORIGINAL")
        val duplikat = lagPayload(msgId = msgId.toString(), saksnummer = "SAK-DUPLIKAT")

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(payload)
        }

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            // lagre samme på nytt
            assertThrows<Exception> { repo.lagre(duplikat) }
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }!!

        assertEquals("SAK-ORIGINAL", lagret.saksnummer)
    }

    @Test
    fun `hentForMsgId returnerer null for ukjent msgId`() {
        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(UUID.randomUUID())
        }

        assertNull(resultat)
    }

    private fun lagPayload(
        msgId: String = UUID.randomUUID().toString(),
        personIdentPasient: String = randomPersonIdent(),
        saksnummer: String = "SAK-001",
        conversationRef: String? = UUID.randomUUID().toString(),
        parentRef: String? = null,
        legehpr: String? = "12345678",
        journalpostId: String = "JP-${UUID.randomUUID()}",
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = DialogmeldingMedSakstilknytning(
        dialogmeldingMottatt = DialogmeldingMottakDTO(
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
            journalpostId = journalpostId,
            fellesformatXML = "<xml/>",
        ),
        sakOgBehandling = BehandlingsflytGateway.SakOgBehandling(saksnummer = saksnummer),
    )

}
