package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.randomHprNummer
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.randomSaksnummer
import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MottattDialogmeldingRepositoryImplTest {

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
        val navnHelsepersonell = "navn helsepersonell"
        val dialogmeldingMottatt = lagMottattDialogmelding(
            tekstNotatInnhold = tekstNotatInnhold,
            dn = dialogmeldingDn,
            navnHelsepersonell = navnHelsepersonell,
        )
        val msgId = UUID.fromString(dialogmeldingMottatt.msgId)

        dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).lagre(dialogmeldingMottatt, saksnummer)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForMsgId(msgId)
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
        assertEquals(dialogmeldingMottatt.mottattTidspunkt.withNano(0), lagret.mottattTidspunkt.withNano(0))
        assertEquals(dialogmeldingMottatt.conversationRef, lagret.conversationRef?.toString())
        assertEquals(dialogmeldingMottatt.parentRef, lagret.parentRef?.toString())
        assertEquals(navnHelsepersonell, lagret.navnHelsepersonell)
    }

    @Test
    fun `dialogmelding med navnHelsepersonell og tekstNotatInnhold`() {
        val dialogmeldingMottatt = lagMottattDialogmelding(
            navnHelsepersonell = "Helt annen behandler",
            tekstNotatInnhold = "Notat fra behandler"
        )
        val msgId = UUID.fromString(dialogmeldingMottatt.msgId)

        dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).lagre(dialogmeldingMottatt, "SAKSNUMMER")
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForMsgId(msgId)
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
            MottattDialogmeldingRepositoryImpl(connection).lagre(dialogmeldingMottatt, saksnummer)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForMsgId(UUID.fromString(dialogmeldingMottatt.msgId))
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
            MottattDialogmeldingRepositoryImpl(connection).lagre(dialogmeldingMottatt, saksnummer)
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForMsgId(UUID.fromString(dialogmeldingMottatt.msgId))
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
            val repo = MottattDialogmeldingRepositoryImpl(connection)
            repo.lagre(dialogmeldingMottatt, "ORIGINAL")
        }

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepositoryImpl(connection)
            // lagre samme på nytt
            assertThrows<Exception> { repo.lagre(duplikat, "DUPLIKAT") }
        }

        val lagret = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForMsgId(msgId)
        }!!

        assertEquals("ORIGINAL", lagret.saksnummer)
    }

    @Test
    fun `hentForMsgId returnerer null for ukjent msgId`() {
        dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).lagre(lagMottattDialogmelding(), randomSaksnummer())
        }
        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForMsgId(UUID.randomUUID())
        }

        assertNull(resultat)
    }

    @Test
    fun `hentForSaksnummer returnerer tom liste når ingen meldinger finnes`() {
        dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).lagre(lagMottattDialogmelding(), randomSaksnummer())
        }
        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForSaksnummer(randomSaksnummer())
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `hentForSaksnummer returnerer alle meldinger med matchende saksnummer`() {
        val saksnummer = randomSaksnummer()
        val melding1 = lagMottattDialogmelding()
        val melding2 = lagMottattDialogmelding()

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepositoryImpl(connection)
            repo.lagre(melding1, saksnummer)
            repo.lagre(melding2, saksnummer)

            // andre
            repo.lagre(
                lagMottattDialogmelding(), randomSaksnummer()
            )
            repo.lagre(
                lagMottattDialogmelding(), randomSaksnummer()
            )
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForSaksnummer(saksnummer)
        }

        assertThat(resultat.map { it.msgId }).containsExactlyInAnyOrder(
            UUID.fromString(melding1.msgId),
            UUID.fromString(melding2.msgId)
        )
    }

    @Test
    fun `hentForSaksnummer filtrerer ut meldinger med et annet saksnummer`() {
        val samtaleRef = UUID.randomUUID()
        val personIdent = randomPersonIdent()
        val forsteSaksnummer = randomSaksnummer()
        val melding = lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)
        val annenMelding =
            lagMottattDialogmelding(conversationRef = UUID.randomUUID().toString(), personIdentPasient = personIdent)

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepositoryImpl(connection)
            repo.lagre(melding, forsteSaksnummer)
            repo.lagre(annenMelding, randomSaksnummer())
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).hentForSaksnummer(forsteSaksnummer)
        }

        assertThat(resultat.map { it.msgId }).containsExactly(UUID.fromString(melding.msgId))
    }

    private fun lagMottattDialogmelding(
        msgId: String = UUID.randomUUID().toString(),
        personIdentPasient: String = randomPersonIdent(),
        conversationRef: String? = UUID.randomUUID().toString(),
        parentRef: String? = UUID.randomUUID().toString(),
        legehpr: String? = randomHprNummer(),
        tekstNotatInnhold: String? = "tekstNotatInnhold",
        dn: String? = "dn",
        navnHelsepersonell: String = "Dr. Testperson",
        journalpostId: String = "JP-${UUID.randomUUID()}",
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = DialogmeldingMottakDTO(
        msgId = msgId,
        msgType = "DIALOG_NOTAT",
        mottattTidspunkt = mottattTidspunkt,
        conversationRef = conversationRef,
        parentRef = parentRef,
        personIdentPasient = personIdentPasient,
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
        journalpostId = journalpostId,
    )
}
