package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.Fakes.behandlingsflytSakResponses
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.pdl.Ident
import dokumentinnhenting.integrasjoner.pdl.PersonIdentGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.randomSaksnummer
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.MottattDialogmeldingRepositoryImpl
import dokumentinnhenting.unleash.FakeUnleashGateway
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.random.Random.Default.nextInt

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FiltrerDialogmeldingUtførerTest {

    private lateinit var dataSource: TestDataSource
    private lateinit var personIdentGateway: PersonIdentGateway

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @BeforeEach
    fun leggTilRandomData() {
        personIdentGateway = mockk()
        every { personIdentGateway.hentAlleIdenterForPerson(any()) } answers {
            listOf(firstArg<Ident>())
        }

        // Lagrer tilfeldig data for bedre treffsikkerhet på asserts i testene
        opprettDialogmelding()
        opprettDialogmelding()
        opprettDialogmelding()
        lagreMottattDialogmelding()
        lagreMottattDialogmelding()
        lagreMottattDialogmelding()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `skal legge til ny jobb når sak finnes, journalpostId er gyldig og svar er satt`() {
        val dto = lagDialogmeldingMottakDTO()
        behandlingsflytSakResponses[dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate()] =
            randomUUID().toString()

        utfør(dto)

        assertThat(hentJobber(dto.personIdentPasient)).hasSize(1)
    }

    @Test
    fun `kobler mottatt melding til riktig sak etter identbytte når conversationRef matcher en sendt melding`() {
        val gammelIdent = randomPersonIdent()
        val nyIdent = randomPersonIdent()
        val saksnummer = randomSaksnummer()

        val sendtMelding = opprettDialogmelding(
            personIdent = gammelIdent,
            saksnummer = saksnummer,
        )
        opprettDialogmelding(
            samtaleRef = sendtMelding.samtaleRef,
            personIdent = gammelIdent,
            saksnummer = saksnummer,
        )
        val mottattMelding = lagDialogmeldingMottakDTO(
            personIdentPasient = nyIdent,
            conversationRef = sendtMelding.samtaleRef.toString(),
            parentRef = null,
        )

        every {
            personIdentGateway.hentAlleIdenterForPerson(Ident(nyIdent))
        } returns listOf(Ident(nyIdent), Ident(gammelIdent, aktivIdent = false))

        utfør(mottattMelding)

        verify(exactly = 1) {
            personIdentGateway.hentAlleIdenterForPerson(Ident(nyIdent))
        }

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `skal ikke legge til jobb når ingen åpen sak finnes`() {
        val personIdent = randomPersonIdent()
        val annenPersonIdent = randomPersonIdent()

        // Sendt og mottatt dialogmelding finnes — men for en annen person
        val samtaleRef = randomUUID()
        opprettDialogmelding(samtaleRef = samtaleRef, personIdent = annenPersonIdent, saksnummer = randomSaksnummer())
        lagreMottattDialogmelding(
            conversationRef = samtaleRef.toString(),
            personIdentPasient = annenPersonIdent,
        )

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = samtaleRef.toString(),
            parentRef = randomUUID().toString(),
            personIdentPasient = personIdent,
        )
        behandlingsflytSakResponses.remove(dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate())

        utfør(dto)

        assertThat(hentJobber(personIdent)).isEmpty()
    }

    @Test
    fun `skal ikke legge til jobb når journalpostId er 0`() {
        mockkObject(Miljø)
        every { Miljø.erDev() } returns true
        try {
            val dto1 = lagDialogmeldingMottakDTO(journalpostId = "0")
            utfør(dto1)
            assertThat(hentJobber(dto1.personIdentPasient)).isEmpty()
        } finally {
            unmockkObject(Miljø)
        }
    }

    @Test
    fun `skal ikke legge til jobb når foresporselSvar er null`() {
        val dto = lagDialogmeldingMottakDTO(foresporselSvar = null)
        behandlingsflytSakResponses[dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate()] =
            randomUUID().toString()

        utfør(dto)

        assertThat(hentJobber(dto.personIdentPasient)).isEmpty()
    }

    @Test
    fun `skal legge til jobb basert på conversationRef når sendt dialogmelding finnes`() {
        val saksnummer = randomSaksnummer()
        val samtaleRef = randomUUID()
        val personIdent = randomPersonIdent()

        opprettDialogmelding(samtaleRef = samtaleRef, personIdent = personIdent, saksnummer = saksnummer)

        val dto = lagDialogmeldingMottakDTO(conversationRef = samtaleRef.toString(), personIdentPasient = personIdent)

        utfør(dto)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `skal legge til jobb basert på parentRef når conversationRef ikke gir treff`() {
        val saksnummer = randomSaksnummer()
        val dialogmeldingUuid = randomUUID()
        val personIdent = randomPersonIdent()

        opprettDialogmelding(uuid = dialogmeldingUuid, personIdent = personIdent, saksnummer = saksnummer)

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = randomUUID().toString(),
            parentRef = dialogmeldingUuid.toString(),
            personIdentPasient = personIdent,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `skal legge til jobb basert på tidligere mottatt dialogmelding med samme conversationRef når conversationRef og parentRef ikke gir treff på sendte dialogmeldinger`() {
        val saksnummer = randomSaksnummer()
        val conversationRef = randomUUID()
        val personIdent = randomPersonIdent()

        lagreMottattDialogmelding(
            conversationRef = conversationRef.toString(),
            personIdentPasient = personIdent,
            saksnummer,
        )

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = conversationRef.toString(),
            parentRef = null,
            personIdentPasient = personIdent,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `skal ikke legge til jobb via tidligere mottatt dialogmelding når ingen mottatt melding finnes med conversationRef`() {
        val personIdent = randomPersonIdent()
        val saksnummer = randomSaksnummer()

        lagreMottattDialogmelding(
            conversationRef = randomUUID().toString(),
            personIdentPasient = personIdent,
            saksnummer,
        )

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = randomUUID().toString(),
            parentRef = null,
            foresporselSvar = null,
            personIdentPasient = personIdent,
        )

        utfør(dto)

        assertThat(hentJobber(dto.personIdentPasient)).isEmpty()
    }

    @Test
    fun `skal ikke legge til jobb via conversationRef når samtaleRef tilhører annen person`() {
        val saksnummer = randomSaksnummer()
        val samtaleRef = randomUUID()
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()

        opprettDialogmelding(samtaleRef = samtaleRef, personIdent = personA, saksnummer = saksnummer)

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = samtaleRef.toString(),
            personIdentPasient = personB,
            foresporselSvar = null,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).isEmpty()
    }

    @Test
    fun `skal ikke legge til jobb via parentRef når parentRef tilhører annen person`() {
        val saksnummer = randomSaksnummer()
        val dialogmeldingUuid = randomUUID()
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()

        opprettDialogmelding(uuid = dialogmeldingUuid, personIdent = personA, saksnummer = saksnummer)

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = randomUUID().toString(),
            parentRef = dialogmeldingUuid.toString(),
            personIdentPasient = personB,
            foresporselSvar = null,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).isEmpty()
    }

    @Test
    fun `skal ikke legge til jobb via tidligere mottatt dialogmelding når mottatt melding tilhører annen person`() {
        val saksnummer = randomSaksnummer()
        val conversationRef = randomUUID()
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()

        lagreMottattDialogmelding(
            conversationRef = conversationRef.toString(),
            personIdentPasient = personA,
            saksnummer,
        )

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = conversationRef.toString(),
            parentRef = null,
            personIdentPasient = personB,
            foresporselSvar = null,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).isEmpty()
    }

    @Test
    fun `skal legge til jobb via conversationRef selv om foresporselSvar er null`() {
        val saksnummer = randomSaksnummer()
        val samtaleRef = randomUUID()
        val personIdent = randomPersonIdent()

        opprettDialogmelding(samtaleRef = samtaleRef, personIdent = personIdent, saksnummer = saksnummer)

        val dto = lagDialogmeldingMottakDTO(
            conversationRef = samtaleRef.toString(),
            personIdentPasient = personIdent,
            foresporselSvar = null,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `skal prøve parentRef når conversationRef ikke er en gyldig UUID`() {
        val saksnummer = randomSaksnummer()
        val dialogmeldingUuid = randomUUID()
        val personIdent = randomPersonIdent()

        opprettDialogmelding(uuid = dialogmeldingUuid, personIdent = personIdent, saksnummer = saksnummer)


        val dto = lagDialogmeldingMottakDTO(
            conversationRef = "ikke-en-uuid",
            parentRef = dialogmeldingUuid.toString(),
            personIdentPasient = personIdent,
        )

        utfør(dto)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `skal falle tilbake til behandlingsflyt-oppslag når hverken conversationRef eller parentRef gir treff`() {
        val dto = lagDialogmeldingMottakDTO(
            conversationRef = randomUUID().toString(),
            parentRef = randomUUID().toString(),
        )
        behandlingsflytSakResponses[dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate()] =
            randomUUID().toString()

        utfør(dto)

        assertThat(hentJobber(dto.personIdentPasient)).hasSize(1)
    }

    @Test
    fun `kobler mottatt melding til riktig sak etter identbytte når parentRef matcher en sendt melding`() {
        val gammelIdent = randomPersonIdent()
        val nyIdent = randomPersonIdent()
        val saksnummer = randomSaksnummer()

        val sendtMelding = opprettDialogmelding(
            personIdent = gammelIdent,
            saksnummer = saksnummer,
        )
        val mottattMelding = lagDialogmeldingMottakDTO(
            personIdentPasient = nyIdent,
            conversationRef = null,
            parentRef = sendtMelding.dialogmeldingUuid.toString(),
        )

        every {
            personIdentGateway.hentAlleIdenterForPerson(Ident(nyIdent))
        } returns listOf(Ident(nyIdent), Ident(gammelIdent, aktivIdent = false))

        utfør(mottattMelding)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `kobler mottatt melding til riktig sak etter identbytte når conversationRef matcher en tidligere mottatt melding`() {
        val gammelIdent = randomPersonIdent()
        val nyIdent = randomPersonIdent()
        val saksnummer = randomSaksnummer()
        val conversationRef = randomUUID().toString()

        lagreMottattDialogmelding(
            conversationRef = conversationRef,
            personIdentPasient = gammelIdent,
            saksnummer = saksnummer,
        )
        lagreMottattDialogmelding(
            conversationRef = conversationRef,
            personIdentPasient = gammelIdent,
            saksnummer = saksnummer,
        )
        val mottattMelding = lagDialogmeldingMottakDTO(
            personIdentPasient = nyIdent,
            conversationRef = conversationRef,
            parentRef = null,
        )

        every {
            personIdentGateway.hentAlleIdenterForPerson(Ident(nyIdent))
        } returns listOf(Ident(nyIdent), Ident(gammelIdent, aktivIdent = false))

        utfør(mottattMelding)

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `når bryter er av brukes bare payload-ident uten PDL-oppslag`() {
        val dto = lagDialogmeldingMottakDTO()
        val saksnummer = randomSaksnummer()
        opprettDialogmelding(
            samtaleRef = UUID.fromString(dto.conversationRef),
            personIdent = dto.personIdentPasient,
            saksnummer = saksnummer,
        )

        utfør(dto, unleashEnabled = false)

        verify(exactly = 0) {
            personIdentGateway.hentAlleIdenterForPerson(any())
        }

        assertThat(hentJobber(saksnummer)).hasSize(1)
    }

    @Test
    fun `PDL-feil feiler behandling uten å opprette jobb`() {
        val dto = lagDialogmeldingMottakDTO()
        val pdlFeil = IllegalArgumentException("PDL-feil")
        every {
            personIdentGateway.hentAlleIdenterForPerson(Ident(dto.personIdentPasient))
        } throws pdlFeil

        val feil = assertThrows<IllegalArgumentException> { utfør(dto) }
        assertThat(feil).isSameAs(pdlFeil)
        verify(exactly = 1) {
            personIdentGateway.hentAlleIdenterForPerson(Ident(dto.personIdentPasient))
        }

        assertThat(hentJobber(dto.personIdentPasient)).isEmpty()
    }

    @Test
    fun `tom PDL-liste feiler behandling uten å opprette jobb`() {
        val dto = lagDialogmeldingMottakDTO()
        every {
            personIdentGateway.hentAlleIdenterForPerson(Ident(dto.personIdentPasient))
        } returns emptyList()

        val feil = assertThrows<IllegalStateException> { utfør(dto) }
        assertThat(feil.message).isEqualTo("PDL returnerte ingen folkeregisteridenter")
        verify(exactly = 1) {
            personIdentGateway.hentAlleIdenterForPerson(Ident(dto.personIdentPasient))
        }

        assertThat(hentJobber(dto.personIdentPasient)).isEmpty()
    }

    @Test
    fun `motstridende saksnumre i sendt samtale feiler behandling`() {
        val dto = lagDialogmeldingMottakDTO(parentRef = null)
        val historiskIdent = randomPersonIdent()
        val samtaleRef = UUID.fromString(dto.conversationRef)
        val førsteSaksnummer = randomSaksnummer()
        val andreSaksnummer = randomSaksnummer()
        opprettDialogmelding(
            samtaleRef = samtaleRef,
            personIdent = dto.personIdentPasient,
            saksnummer = førsteSaksnummer,
        )
        opprettDialogmelding(
            samtaleRef = samtaleRef,
            personIdent = historiskIdent,
            saksnummer = andreSaksnummer,
        )
        every {
            personIdentGateway.hentAlleIdenterForPerson(any())
        } returns listOf(Ident(dto.personIdentPasient), Ident(historiskIdent, aktivIdent = false))

        val feil = assertThrows<IllegalStateException> { utfør(dto) }
        assertThat(feil.message)
            .isEqualTo("Fant flere saksnumre for samme samtale i sendte dialogmeldinger")

        assertThat(hentJobber(dto.personIdentPasient)).isEmpty()
    }

    @Test
    fun `motstridende saksnumre i mottatt samtale stopper behandling`() {
        val dto = lagDialogmeldingMottakDTO(parentRef = null)
        val historiskIdent = randomPersonIdent()
        val samtaleRef = dto.conversationRef
        val førsteSaksnummer = randomSaksnummer()
        val andreSaksnummer = randomSaksnummer()
        lagreMottattDialogmelding(
            conversationRef = samtaleRef,
            personIdentPasient = dto.personIdentPasient,
            saksnummer = førsteSaksnummer,
        )
        lagreMottattDialogmelding(
            conversationRef = samtaleRef,
            personIdentPasient = historiskIdent,
            saksnummer = andreSaksnummer,
        )
        every {
            personIdentGateway.hentAlleIdenterForPerson(any())
        } returns listOf(Ident(dto.personIdentPasient), Ident(historiskIdent, aktivIdent = false))

        val feil = assertThrows<IllegalStateException> { utfør(dto) }
        assertThat(feil.message)
            .isEqualTo("Fant flere saksnumre for samme samtale i mottatte dialogmeldinger")

        assertThat(hentJobber(dto.personIdentPasient)).isEmpty()
    }

    private fun utfør(dto: DialogmeldingMottakDTO, unleashEnabled: Boolean = true) {
        dataSource.transaction { connection ->
            FiltrerDialogmeldingUtfører(
                FlytJobbRepository(connection),
                DialogmeldingRepositoryImpl(connection),
                MottattDialogmeldingRepositoryImpl(connection),
                personIdentGateway,
                FakeUnleashGateway(enabled = unleashEnabled),
            )
                .utfør(lagJobbInput(dto))
        }
    }

    private fun hentJobber(innhold: String): List<String> =
        dataSource.transaction { connection ->
            connection.queryList("SELECT PAYLOAD FROM JOBB WHERE TYPE = ? AND PAYLOAD LIKE ?") {
                setParams {
                    setString(1, "dialogmelding.handler")
                    setString(2, "%$innhold%")
                }
                setRowMapper { it.getString("PAYLOAD") }
            }
        }

    private fun lagJobbInput(dto: DialogmeldingMottakDTO): JobbInput =
        JobbInput(FiltrerDialogmeldingUtfører).medPayload(DefaultJsonMapper.toJson(dto))

    private fun lagDialogmeldingMottakDTO(
        journalpostId: String = randomUUID().toString(),
        foresporselSvar: ForesporselFraSaksbehandlerForesporselSvar? = lagForesporselSvar(),
        conversationRef: String? = randomUUID().toString(),
        parentRef: String? = randomUUID().toString(),
        personIdentPasient: String = randomPersonIdent(),
    ) = DialogmeldingMottakDTO(
        msgId = randomUUID().toString(),
        msgType = randomUUID().toString(),
        mottattTidspunkt = LocalDateTime.now(),
        conversationRef = conversationRef,
        parentRef = parentRef,
        personIdentPasient = personIdentPasient,
        legehpr = null,
        dialogmelding = Dialogmelding(
            id = randomUUID().toString(),
            innkallingMoterespons = null,
            foresporselFraSaksbehandlerForesporselSvar = foresporselSvar,
            henvendelseFraLegeHenvendelse = null,
            navnHelsepersonell = randomUUID().toString(),
            signaturDato = LocalDateTime.now(),
        ),
        journalpostId = journalpostId,
    )

    private fun lagForesporselSvar() = ForesporselFraSaksbehandlerForesporselSvar(
        temaKode = TemaKode(
            kodeverkOID = randomUUID().toString(),
            dn = randomUUID().toString(),
            v = randomUUID().toString(),
            arenaNotatKategori = randomUUID().toString(),
            arenaNotatKode = randomUUID().toString(),
            arenaNotatTittel = randomUUID().toString(),
        ),
        tekstNotatInnhold = randomUUID().toString(),
        dokIdNotat = null,
        datoNotat = null,
    )

    private fun opprettDialogmelding(
        uuid: UUID = randomUUID(),
        samtaleRef: UUID = randomUUID(),
        personIdent: String = randomPersonIdent(),
        saksnummer: String = randomSaksnummer(),
    ): DialogmeldingRecord {
        val record = lagDialogmeldingRecord(
            uuid = uuid,
            samtaleRef = samtaleRef,
            personIdent = personIdent,
            saksnummer = saksnummer
        )

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }
        return record
    }

    private fun lagDialogmeldingRecord(
        uuid: UUID = randomUUID(),
        samtaleRef: UUID = randomUUID(),
        personIdent: String = randomPersonIdent(),
        saksnummer: String = randomSaksnummer(),
    ) = DialogmeldingRecord(
        bestillerNavIdent = "Z123456",
        dialogmeldingUuid = uuid,
        behandlerRef = randomUUID().toString(),
        behandlerHprNr = nextInt(100000, 999999).toString(),
        personIdent = personIdent,
        personNavn = "Test Testesen",
        saksnummer = saksnummer,
        dokumentasjonType = DokumentasjonType.L40,
        behandlerNavn = "Behandler Navn",
        fritekst = "fritekst",
        behandlingsReferanse = randomUUID(),
        samtaleRef = samtaleRef,
    )

    private fun lagreMottattDialogmelding(
        conversationRef: String? = randomUUID().toString(),
        personIdentPasient: String = randomPersonIdent(),
        saksnummer: String = randomSaksnummer(),
    ): DialogmeldingMottakDTO {
        val tidligereMottatt = lagDialogmeldingMottakDTO(
            conversationRef = conversationRef,
            personIdentPasient = personIdentPasient,
        )
        dataSource.transaction { connection ->
            MottattDialogmeldingRepositoryImpl(connection).lagre(tidligereMottatt, saksnummer)
        }
        return tidligereMottatt
    }
}
