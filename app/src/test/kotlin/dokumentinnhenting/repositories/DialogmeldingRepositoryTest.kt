package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDto
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.randomNavIdent
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DialogmeldingRepositoryTest {

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    private fun lagRecord(
        uuid: UUID = UUID.randomUUID(),
        behandlingsreferanse: UUID = UUID.randomUUID(),
        saksnummer: String = "SAK-001",
        tidligereBestillingReferanse: UUID? = null,
        personIdent: String = randomNavIdent(),
        samtaleRef: UUID = UUID.randomUUID(),
        dokumentasjonType: DokumentasjonType = DokumentasjonType.L8
    ) = DialogmeldingRecord(
        bestillerNavIdent = randomNavIdent(),
        dialogmeldingUuid = uuid,
        behandlingsReferanse = behandlingsreferanse,
        behandlerRef = "behandlerRef-123",
        behandlerHprNr = "12344321",
        personIdent = personIdent,
        personNavn = "Ola Nordmann",
        saksnummer = saksnummer,
        dokumentasjonType = dokumentasjonType,
        behandlerNavn = "Dr. Behandler",
        fritekst = "En fritekst",
        tidligereBestillingReferanse = tidligereBestillingReferanse,
        samtaleRef = samtaleRef,
    )

    @Test
    fun `opprettDialogmelding lagrer melding og returnerer uuid`() {
        val record = lagRecord()

        val returnertUuid = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        assertEquals(record.dialogmeldingUuid, returnertUuid)

        val lagret = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(record.dialogmeldingUuid, lagret.dialogmeldingUuid)
        assertEquals(record.saksnummer, lagret.saksnummer)
        assertEquals(record.personIdent, lagret.personIdent)
        assertEquals(record.behandlerRef, lagret.behandlerRef)
        assertEquals(record.bestillerNavIdent, lagret.bestillerNavIdent)
        assertEquals(record.behandlerNavn, lagret.behandlerNavn)
        assertEquals(record.behandlerHprNr, lagret.behandlerHprNr)
        assertEquals(record.dokumentasjonType, lagret.dokumentasjonType)
        assertEquals(record.fritekst, lagret.fritekst)
        assertNull(lagret.status)
        assertNull(lagret.flytStatus)
    }

    @Test
    fun `eksisterer returnerer true når dialogmelding finnes`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val eksisterer = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).eksisterer(record.dialogmeldingUuid)
        }

        assertTrue(eksisterer)
    }

    @Test
    fun `eksisterer returnerer false når dialogmelding ikke finnes`() {
        val eksisterer = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).eksisterer(UUID.randomUUID())
        }

        assertFalse(eksisterer)
    }

    @Test
    fun `hentByDialogId returnerer null for ukjent uuid`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentByDialogId(UUID.randomUUID())
        }

        assertNull(resultat)
    }

    @Test
    fun `hentBySaksnummer returnerer alle meldinger for saksnummer`() {
        val saksnummer = "SAK-HENT-001"
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()

        dataSource.transaction { connection ->
            val repo = DialogmeldingRepository(connection)
            repo.opprettDialogmelding(lagRecord(uuid = uuid1, saksnummer = saksnummer))
            repo.opprettDialogmelding(lagRecord(uuid = uuid2, saksnummer = saksnummer))
            repo.opprettDialogmelding(lagRecord(saksnummer = "ANNEN-SAK"))
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentBySaksnummer(saksnummer)
        }

        assertEquals(2, resultat.size)
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid1 })
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid2 })
    }

    @Test
    fun `hentBySaksnummer returnerer tom liste for ukjent saksnummer`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentBySaksnummer("UKJENT-SAK")
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `oppdaterDialogmeldingStatus setter status og tekst`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val statusDto = DialogmeldingStatusDto(
            uuid = record.dialogmeldingUuid.toString(),
            createdAt = OffsetDateTime.now(),
            status = MeldingStatusType.SENDT,
            tekst = "Sendt til behandler",
            bestillingUuid = record.dialogmeldingUuid.toString(),
        )

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).oppdaterDialogmeldingStatus(statusDto)
        }

        val oppdatert = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(MeldingStatusType.SENDT, oppdatert.status)
        assertEquals("Sendt til behandler", oppdatert.statusTekst)
    }

    @Test
    fun `leggTilJournalpostPåBestilling oppdaterer journalpost og dokument id`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection)
                .leggTilJournalpostPåBestilling(record.dialogmeldingUuid, "JP-123", "DOK-456")
        }

        val oppdatert = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals("JP-123", oppdatert.journalpostId)
        assertEquals("DOK-456", oppdatert.dokumentId)
    }

    @Test
    fun `oppdaterFlytStatus setter flytstatus`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection)
                .oppdaterFlytStatus(record.dialogmeldingUuid, ProsesseringSyfoStatus.SENDT_TIL_SYFO)
        }

        val oppdatert = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(ProsesseringSyfoStatus.SENDT_TIL_SYFO, oppdatert.flytStatus)
    }

    @Test
    fun `hentFlytStatus returnerer korrekt flytstatus etter oppdatering`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            val repo = DialogmeldingRepository(connection)
            repo.opprettDialogmelding(record)
            repo.oppdaterFlytStatus(record.dialogmeldingUuid, ProsesseringSyfoStatus.JOURNALFØRT)
        }

        val flytStatus = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentFlytStatus(record.dialogmeldingUuid)
        }

        assertEquals(record.dialogmeldingUuid, flytStatus.dialogmeldingUuid)
        assertEquals(record.saksnummer, flytStatus.saksnummer)
        assertEquals(ProsesseringSyfoStatus.JOURNALFØRT, flytStatus.flytStatus)
    }

    @Test
    fun `hentFlytStatus har null flytstatus etter opprettelse`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val flytStatus = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentFlytStatus(record.dialogmeldingUuid)
        }

        assertNull(flytStatus.flytStatus)
    }

    @Test
    fun `hentBestillingEldreEnn14Dager returnerer null for ny bestilling`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentBestillingEldreEnn14Dager(record.dialogmeldingUuid)
        }

        assertNull(resultat)
    }

    @Test
    fun `låsBestilling returnerer korrekt uuid`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val låstUuid = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).låsBestilling(record.dialogmeldingUuid)
        }

        assertEquals(record.dialogmeldingUuid, låstUuid)
    }

    @Test
    fun `opprettDialogmelding lagrer tidligereBestillingReferanse`() {
        val tidligereUuid = UUID.randomUUID()
        val record = lagRecord(tidligereBestillingReferanse = tidligereUuid)

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val lagret = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(tidligereUuid, lagret.tidligereBestillingReferanse)
    }

    @Test
    fun `hentForParent returnerer melding når parentRef og personIdent matcher`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(lagRecord())
            DialogmeldingRepository(connection).opprettDialogmelding(record)
            DialogmeldingRepository(connection).opprettDialogmelding(lagRecord())
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForParent(record.dialogmeldingUuid, record.personIdent)
        }

        assertEquals(record.dialogmeldingUuid, resultat?.dialogmeldingUuid)
    }

    @Test
    fun `hentForParent returnerer null for ukjent parentRef`() {
        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(lagRecord())
        }
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForParent(UUID.randomUUID(), randomPersonIdent())
        }

        assertNull(resultat)
    }

    @Test
    fun `hentForParent returnerer null når parentRef tilhører annen person`() {
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()
        val record = lagRecord(personIdent = personA)

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(record)
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForParent(record.dialogmeldingUuid, personB)
        }

        assertNull(resultat)
    }

    @Test
    fun `hentForSamtale returnerer alle meldinger for samtaleRef og personIdent`() {
        val personIdent = randomPersonIdent()
        val samtaleRef = UUID.randomUUID()
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()

        dataSource.transaction { connection ->
            val repo = DialogmeldingRepository(connection)
            repo.opprettDialogmelding(lagRecord(uuid = uuid1, personIdent = personIdent, samtaleRef = samtaleRef))
            repo.opprettDialogmelding(lagRecord(uuid = uuid2, personIdent = personIdent, samtaleRef = samtaleRef))
            repo.opprettDialogmelding(lagRecord(personIdent = personIdent, samtaleRef = UUID.randomUUID()))
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForSamtale(samtaleRef, personIdent)
        }

        assertEquals(2, resultat.size)
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid1 })
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid2 })
    }

    @Test
    fun `hentForSamtale returnerer tom liste for ukjent samtaleRef`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForSamtale(UUID.randomUUID(), randomPersonIdent())
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `hentForSamtale returnerer tom liste når samtaleRef tilhører annen person`() {
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()
        val samtaleRef = UUID.randomUUID()

        dataSource.transaction { connection ->
            DialogmeldingRepository(connection)
                .opprettDialogmelding(lagRecord(personIdent = personA, samtaleRef = samtaleRef))
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForSamtale(samtaleRef, personB)
        }

        assertTrue(resultat.isEmpty())
    }


    @Test
    fun `hent bestillinger gitt behandlingsreferanse`() {
        val behandlingsreferanse = BehandlingReferanse(UUID.randomUUID())
        val recordLegeerklæring = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40)
        val recordTilleggsopplysning = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L8)


        dataSource.transaction { connection ->
            DialogmeldingRepository(connection).opprettDialogmelding(recordLegeerklæring)
            DialogmeldingRepository(connection).opprettDialogmelding(recordTilleggsopplysning)
        }

        val bestillinger = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentBestillingerForDokumentasjonstyper(behandlingsreferanse, dokumentasjonstyper = listOf(
                DokumentasjonType.L40, DokumentasjonType.L8))
        }

        assertThat(bestillinger).hasSize(2)
        assertThat(bestillinger.all { it.behandlingsReferanse == behandlingsreferanse.referanse }).isTrue()
        assertThat(bestillinger.map { it.dokumentasjonType }).containsExactlyInAnyOrder(DokumentasjonType.L40, DokumentasjonType.L8)

        val bestillingerBareL40 = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentBestillingerForDokumentasjonstyper(behandlingsreferanse, dokumentasjonstyper = listOf(
                DokumentasjonType.L40))
        }

        assertThat(bestillingerBareL40).hasSize(1)
        assertThat(bestillingerBareL40.first().dokumentasjonType).isEqualTo(DokumentasjonType.L40)

    }
}
