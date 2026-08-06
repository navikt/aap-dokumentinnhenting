package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.randomPersonIdent
import io.mockk.clearAllMocks
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
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

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `lagre dialogmelding og hentForMsgId returnerer den`() {
        val saksnummer = UUID.randomUUID().toString()
        val tekstNotatInnhold = "tekst notat innhold"
        val dialogmeldingDn = "dialogmelding dn"
        val dialogmeldingMottatt = lagMottattDialogmelding(
            tekstNotatInnhold = tekstNotatInnhold,
            dn = dialogmeldingDn
        )
        val msgId = UUID.fromString(dialogmeldingMottatt.msgId)

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(dialogmeldingMottatt, saksnummer)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }

        assertNotNull(lagret)
        assertEquals(msgId, lagret!!.msgId)
        assertEquals(dialogmeldingMottatt.msgType, lagret.msgType)
        assertEquals(dialogmeldingMottatt.personIdentPasient, lagret.personIdentPasient)
        assertEquals(dialogmeldingMottatt.journalpostId, lagret.journalpostId)
        assertEquals(dialogmeldingMottatt.legehpr, lagret.legehpr)
        assertEquals(saksnummer, lagret.saksnummer)
        assertEquals(DialogmeldingType.FORESPORSEL_SVAR, lagret.dialogmeldingType)
        assertEquals(tekstNotatInnhold, lagret.tekstNotatInnhold)
        assertEquals(dialogmeldingDn, lagret.dn)
    }

    @Test
    fun `dialogmelding med navnHelsepersonell og tekstNotatInnhold`() {
        val dialogmeldingMottatt = lagMottattDialogmelding(
            navnHelsepersonell = "Helt annen behandler",
            tekstNotatInnhold = "Notat fra behandler"
        )
        val msgId = UUID.fromString(dialogmeldingMottatt.msgId)

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(dialogmeldingMottatt, "SAKSNUMMER")
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }

        assertEquals("Helt annen behandler", lagret!!.navnHelsepersonell)
        assertEquals("Notat fra behandler", lagret.tekstNotatInnhold)
    }

    @Test
    fun `lagre lagrer conversationRef og parentRef korrekt`() {
        val saksnummer = UUID.randomUUID().toString()
        val conversationRef = UUID.randomUUID()
        val parentRef = UUID.randomUUID()
        val dialogmeldingMottatt = lagMottattDialogmelding(
            conversationRef = conversationRef.toString(),
            parentRef = parentRef.toString(),
        )

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(dialogmeldingMottatt, saksnummer)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(UUID.fromString(dialogmeldingMottatt.msgId))
        }!!

        assertEquals(conversationRef, lagret.conversationRef)
        assertEquals(parentRef, lagret.parentRef)
    }

    @Test
    fun `lagre håndterer null-felter for conversationRef, parentRef og legehpr`() {
        val saksnummer = UUID.randomUUID().toString()
        val dialogmeldingMottatt = lagMottattDialogmelding(
            conversationRef = null,
            parentRef = null,
            legehpr = null,
        )

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).lagre(dialogmeldingMottatt, saksnummer)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(UUID.fromString(dialogmeldingMottatt.msgId))
        }!!

        assertNull(lagret.conversationRef)
        assertNull(lagret.parentRef)
        assertNull(lagret.legehpr)
    }

    @Test
    fun `duplikat med samme msgId kaster exception`() {
        val msgId = UUID.randomUUID()
        val dialogmeldingMottatt = lagMottattDialogmelding(msgId = msgId.toString())
        val duplikat = lagMottattDialogmelding(msgId = msgId.toString())

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(dialogmeldingMottatt, "ORIGINAL")
        }

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            // lagre samme på nytt
            assertThrows<Exception> { repo.lagre(duplikat, "DUPLIKAT") }
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(msgId)
        }!!

        assertEquals("ORIGINAL", lagret.saksnummer)
    }

    @Test
    fun `hentForMsgId returnerer null for ukjent msgId`() {
        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForMsgId(UUID.randomUUID())
        }

        assertNull(resultat)
    }

    private fun lagMottattDialogmelding(
        msgId: String = UUID.randomUUID().toString(),
        personIdentPasient: String = randomPersonIdent(),
        conversationRef: String? = UUID.randomUUID().toString(),
        parentRef: String? = null,
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
}
