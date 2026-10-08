package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
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
        val navnHelsepersonell = "navn helsepersonell"
        val dialogmeldingMottatt = lagMottattDialogmelding(
            tekstNotatInnhold = tekstNotatInnhold,
            dn = dialogmeldingDn,
            navnHelsepersonell = navnHelsepersonell,
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

    @Test
    fun `hentForSamtale returnerer tom liste når ingen meldinger finnes`() {
        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSamtale(UUID.randomUUID(), randomPersonIdent())
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `hentForSamtale returnerer alle meldinger med matchende samtaleRef og personIdent`() {
        val samtaleRef = UUID.randomUUID()
        val personIdent = randomPersonIdent()
        val melding1 =
            lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)
        val melding2 =
            lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)


        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(melding1, randomSaksnummer())
            repo.lagre(melding2, randomSaksnummer())

            // andre
            repo.lagre(
                lagMottattDialogmelding(
                    conversationRef = UUID.randomUUID().toString(),
                    personIdentPasient = randomPersonIdent()
                ), randomSaksnummer()
            )
            repo.lagre(
                lagMottattDialogmelding(
                    conversationRef = UUID.randomUUID().toString(),
                    personIdentPasient = randomPersonIdent()
                ), randomSaksnummer()
            )
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSamtale(samtaleRef, personIdent)
        }

        assertThat(resultat.map { it.msgId }).containsExactlyInAnyOrder(
            UUID.fromString(melding1.msgId),
            UUID.fromString(melding2.msgId)
        )
    }

    @Test
    fun `hentForSamtale filtrerer ut meldinger med annen samtaleRef`() {
        val samtaleRef = UUID.randomUUID()
        val personIdent = randomPersonIdent()
        val melding = lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)
        val annenMelding =
            lagMottattDialogmelding(conversationRef = UUID.randomUUID().toString(), personIdentPasient = personIdent)

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(melding, randomSaksnummer())
            repo.lagre(annenMelding, randomSaksnummer())
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSamtale(samtaleRef, personIdent)
        }

        assertThat(resultat.map { it.msgId }).containsExactly(UUID.fromString(melding.msgId))
    }

    @Test
    fun `hentForSamtale filtrerer ut meldinger med annen personIdent`() {
        val samtaleRef = UUID.randomUUID()
        val personIdentA = randomPersonIdent()
        val personIdentB = randomPersonIdent()
        val meldingA =
            lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdentA)
        val meldingB =
            lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdentB)

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(meldingA, randomSaksnummer())
            repo.lagre(meldingB, randomSaksnummer())
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSamtale(samtaleRef, personIdentA)
        }

        assertThat(resultat.map { it.msgId }).containsExactly(UUID.fromString(meldingA.msgId))
    }

    @Test
    fun `hentForSaksnummer returnerer tom liste når ingen meldinger finnes`() {
        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSaksnummer(randomSaksnummer())
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `hentForSaksnummer returnerer alle meldinger med matchende samtaleRef og personIdent`() {
        val samtaleRef = UUID.randomUUID()
        val personIdent = randomPersonIdent()
        val saksnummer = randomSaksnummer()
        val melding1 =
            lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)
        val melding2 =
            lagMottattDialogmelding(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)

        dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(melding1, saksnummer)
            repo.lagre(melding2, saksnummer)

            // andre
            repo.lagre(
                lagMottattDialogmelding(
                    conversationRef = UUID.randomUUID().toString(),
                    personIdentPasient = randomPersonIdent()
                ), randomSaksnummer()
            )
            repo.lagre(
                lagMottattDialogmelding(
                    conversationRef = UUID.randomUUID().toString(),
                    personIdentPasient = randomPersonIdent()
                ), randomSaksnummer()
            )
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSaksnummer(saksnummer)
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
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(melding, forsteSaksnummer)
            repo.lagre(annenMelding, randomSaksnummer())
        }

        val resultat = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSaksnummer(forsteSaksnummer)
        }

        assertThat(resultat.map { it.msgId }).containsExactly(UUID.fromString(melding.msgId))
    }

    @Test
    fun `oppdaterPersonIdentPåSamtaleRef oppdaterer kun gammel ident på alle i samme samtale`() {
        val saksnummer = randomSaksnummer()
        val samtaleRef = UUID.randomUUID()
        val samtaleRefString = samtaleRef.toString()
        val annenSamtaleRef = UUID.randomUUID()
        val annenSamtaleRefString = annenSamtaleRef.toString()
        val nyPersonIdent = randomPersonIdent()
        val gammelIdent = randomPersonIdent()
        val annenPersonIdent = randomPersonIdent()
        val identHistorikk = listOf(nyPersonIdent, gammelIdent)

        val meldingNyIdent =
            lagMottattDialogmelding(personIdentPasient = nyPersonIdent, conversationRef = samtaleRefString)
        val meldingGammelIdent =
            lagMottattDialogmelding(personIdentPasient = gammelIdent, conversationRef = samtaleRefString)
        val meldingAnnenPerson =
            lagMottattDialogmelding(personIdentPasient = annenPersonIdent, conversationRef = samtaleRefString)
        val meldingAnnenSamtale =
            lagMottattDialogmelding(personIdentPasient = gammelIdent, conversationRef = annenSamtaleRefString)

        val meldingerNyIdentFørEndring = dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(meldingNyIdent, saksnummer)
            repo.lagre(meldingGammelIdent, saksnummer)
            repo.lagre(meldingAnnenPerson, saksnummer)
            repo.lagre(meldingAnnenSamtale, saksnummer)
            repo.hentForSamtale(samtaleRef, nyPersonIdent)
        }

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).oppdaterPersonIdentPåSamtaleRef(
                samtaleRef,
                identHistorikk,
                nyPersonIdent
            )
        }

        val (meldingerNyIdentEtterEndring, meldingerAnnenSamtaleEtterEndring) = dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            val meldingerSamtale = repo.hentForSamtale(samtaleRef, nyPersonIdent)
            val meldingerAnnenSamtale = repo.hentForSamtale(annenSamtaleRef, nyPersonIdent)
            meldingerSamtale to meldingerAnnenSamtale
        }

        assertThat(meldingerNyIdentFørEndring.size).isEqualTo(1)
        assertThat(meldingerNyIdentEtterEndring.size).isEqualTo(2)
        assertThat(meldingerAnnenSamtaleEtterEndring.size).isEqualTo(0)
    }

    @Test
    fun `oppdaterPersonIdentPåParentRef oppdaterer kun gammel ident på parent`() {
        val saksnummer = randomSaksnummer()
        val parentRef = UUID.randomUUID()
        val parentRefString = parentRef.toString()
        val nyPersonIdent = randomPersonIdent()
        val gammelIdent = randomPersonIdent()
        val identHistorikk = listOf(nyPersonIdent, gammelIdent)

        val meldingParent = lagMottattDialogmelding(personIdentPasient = gammelIdent, msgId = parentRefString)
        val meldingNyIdent = lagMottattDialogmelding(personIdentPasient = nyPersonIdent, parentRef = parentRefString)
        val meldingGammelIdent = lagMottattDialogmelding(personIdentPasient = gammelIdent, parentRef = parentRefString)

        val meldingerParentNyIdentFørEndring = dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.lagre(meldingParent, saksnummer)
            repo.lagre(meldingNyIdent, saksnummer)
            repo.lagre(meldingGammelIdent, saksnummer)
            repo.hentForParent(parentRef, nyPersonIdent)
        }

        dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).oppdaterPersonIdentPåParentRef(
                parentRef,
                identHistorikk,
                nyPersonIdent
            )
        }

        val meldingerParentNyIdentEtterEndring = dataSource.transaction { connection ->
            val repo = MottattDialogmeldingRepository(connection)
            repo.hentForParent(parentRef, nyPersonIdent)
        }

        assertThat(meldingerParentNyIdentFørEndring.size).isEqualTo(0)
        assertThat(meldingerParentNyIdentEtterEndring.size).isEqualTo(1)
    }

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
}
