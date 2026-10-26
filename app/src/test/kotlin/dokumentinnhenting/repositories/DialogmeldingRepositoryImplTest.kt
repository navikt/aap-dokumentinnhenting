package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.domene.Behandler
import dokumentinnhenting.domene.Forespørsel
import dokumentinnhenting.domene.Journalreferanse
import dokumentinnhenting.domene.Person
import dokumentinnhenting.domene.Statushendelse
import dokumentinnhenting.domene.UtgåendeDialogmelding
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDto
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.randomNavIdent
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.randomSaksnummer
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.UUID.randomUUID

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DialogmeldingRepositoryImplTest {

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
    fun `påminnelseoppslag har indeks på tidligere bestilling og dokumentasjonstype`() {
        val indeksdefinisjon = dataSource.transaction { connection ->
            connection.queryFirst(
                """
                    SELECT indexdef FROM pg_indexes
                    WHERE schemaname = current_schema()
                      AND tablename = 'dialogmelding'
                      AND indexname = 'idx_dialogmelding_tidligere_bestilling_dokumentasjontype'
                """.trimIndent()
            ) {
                setRowMapper { it.getString("indexdef") }
            }
        }

        assertThat(indeksdefinisjon)
            .contains("USING btree (tidligere_bestilling_referanse, dokumentasjontype)")
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
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        assertEquals(record.dialogmeldingUuid, returnertUuid)

        val lagret = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentByDialogId(record.dialogmeldingUuid)
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
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val eksisterer = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).eksisterer(record.dialogmeldingUuid)
        }

        assertTrue(eksisterer)
    }

    @Test
    fun `eksisterer returnerer false når dialogmelding ikke finnes`() {
        val eksisterer = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).eksisterer(UUID.randomUUID())
        }

        assertFalse(eksisterer)
    }

    @Test
    fun `hentByDialogId returnerer null for ukjent uuid`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentByDialogId(UUID.randomUUID())
        }

        assertNull(resultat)
    }

    @Test
    fun `hentForespørsel bevarer dagens tilstand uten historikk`() {
        val record = lagRecord()
        val status = DialogmeldingStatusDto(
            uuid = randomUUID().toString(),
            createdAt = OffsetDateTime.now(),
            status = MeldingStatusType.OK,
            tekst = "Levert til behandler",
            bestillingUuid = record.dialogmeldingUuid.toString(),
        )
        dataSource.transaction { connection ->
            val repository = DialogmeldingRepositoryImpl(connection)
            repository.opprettDialogmelding(record)
            repository.oppdaterDialogmeldingStatus(status)
            repository.oppdaterFlytStatus(record.dialogmeldingUuid, ProsesseringSyfoStatus.FERDIG)
            repository.leggTilJournalpostPåBestilling(record.dialogmeldingUuid, "123456", "654321")
            repository.settAutomatiskPåminnelse(false, record.dialogmeldingUuid)

            val bestilling = requireNotNull(repository.hentForespørsel(record.dialogmeldingUuid))
            val forventet = Forespørsel(
                melding = UtgåendeDialogmelding(
                    id = record.dialogmeldingUuid,
                    behandlingsreferanse = BehandlingReferanse(record.behandlingsReferanse),
                    saksnummer = Saksnummer(record.saksnummer),
                    person = Person(record.personIdent, record.personNavn),
                    behandler = Behandler(record.behandlerRef, record.behandlerHprNr, record.behandlerNavn),
                    bestillerNavIdent = record.bestillerNavIdent,
                    tekst = record.fritekst,
                    opprettetTidspunkt = requireNotNull(repository.hentByDialogId(record.dialogmeldingUuid)).opprettet,
                    journalreferanse = Journalreferanse(JournalpostId("123456"), "654321"),
                    gjeldendeStatus = MeldingStatusType.OK,
                    statusTekst = status.tekst,
                    samtaleRef = record.samtaleRef,
                    flytStatus = ProsesseringSyfoStatus.FERDIG,
                    statushistorikk = emptyList(),
                ),
                dokumentasjonstype = record.dokumentasjonType,
                automatiskPåminnelseAktiv = false,
                påminnelser = emptyList(),
            )
            assertThat(bestilling).usingRecursiveComparison().isEqualTo(forventet)
        }
    }

    @Test
    fun `hentForespørsel kobler alle påminnelser og deres egen delvise historikk`() {
        val original = lagRecord()
        val første = lagRecord(
            tidligereBestillingReferanse = original.dialogmeldingUuid,
            dokumentasjonType = DokumentasjonType.PURRING,
        )
        val siste = lagRecord(
            tidligereBestillingReferanse = original.dialogmeldingUuid,
            dokumentasjonType = DokumentasjonType.PURRING,
        )
        val tidspunkt = OffsetDateTime.parse("2026-10-23T10:00:00+02:00")
        val originalhendelse = DialogmeldingStatusDto(
            uuid = randomUUID().toString(),
            createdAt = tidspunkt,
            status = MeldingStatusType.OK,
            tekst = "Levert",
            bestillingUuid = original.dialogmeldingUuid.toString(),
        )
        val purringhendelse = originalhendelse.copy(
            uuid = randomUUID().toString(),
            bestillingUuid = siste.dialogmeldingUuid.toString(),
            status = MeldingStatusType.SENDT,
            tekst = null,
        )
        val senereHendelse = purringhendelse.copy(
            uuid = randomUUID().toString(),
            createdAt = tidspunkt.plusMinutes(10),
            status = MeldingStatusType.OK,
        )

        dataSource.transaction { connection ->
            val repository = DialogmeldingRepositoryImpl(connection)
            listOf(
                original, første, siste,
                lagRecord(tidligereBestillingReferanse = original.dialogmeldingUuid),
                lagRecord(dokumentasjonType = DokumentasjonType.PURRING),
            ).forEach { repository.opprettDialogmelding(it) }
            repository.oppdaterDialogmeldingStatus(originalhendelse)
            repository.oppdaterDialogmeldingStatus(purringhendelse)
            // Lagringsrekkefølge og siste historikkhendelse skal ikke bestemme gjeldende status.
            listOf(senereHendelse, originalhendelse, purringhendelse).forEach {
                repository.lagreStatusHistorikk(it, tidspunkt.toInstant().plusSeconds(1200))
            }

            val bestilling = requireNotNull(repository.hentForespørsel(original.dialogmeldingUuid))
            assertThat(bestilling.påminnelser.map { it.melding.id })
                .containsExactlyInAnyOrder(første.dialogmeldingUuid, siste.dialogmeldingUuid)
            assertThat(bestilling.påminnelser.map { it.forespørselId })
                .containsOnly(original.dialogmeldingUuid)
            assertThat(bestilling.harPåminnelse()).isTrue()
            assertThat(bestilling.melding.statushistorikk.map { it.kildehendelseId })
                .containsExactly(originalhendelse.uuid)
            assertThat(bestilling.påminnelser.single { it.melding.id == første.dialogmeldingUuid }
                .melding.statushistorikk).isEmpty()
            val purring = bestilling.påminnelser.single { it.melding.id == siste.dialogmeldingUuid }.melding
            assertThat(purring.gjeldendeStatus).isEqualTo(MeldingStatusType.SENDT)
            assertThat(purring.statushistorikk).usingRecursiveComparison().isEqualTo(
                listOf(purringhendelse, senereHendelse).map {
                    Statushendelse(
                        kildehendelseId = it.uuid,
                        status = it.status,
                        hendelseTidspunkt = it.createdAt.toInstant(),
                        mottattTidspunkt = tidspunkt.toInstant().plusSeconds(1200),
                        tekst = it.tekst,
                    )
                }
            )
            assertThat(purring.journalreferanse).isNull()
        }
    }

    @Test
    fun `hentForespørsel bevarer delvise journalreferanser og returnerer null for ukjent forespørsel`() {
        dataSource.transaction { connection ->
            val repository = DialogmeldingRepositoryImpl(connection)
            assertThat(repository.hentForespørsel(randomUUID())).isNull()
            val record = lagRecord()
            repository.opprettDialogmelding(record)
            connection.execute("UPDATE DIALOGMELDING SET JOURNALPOST_ID = ? WHERE DIALOGMELDING_UUID = ?") {
                setParams {
                    setString(1, "123456")
                    setUUID(2, record.dialogmeldingUuid)
                }
            }
            val journalreferanse = requireNotNull(repository.hentForespørsel(record.dialogmeldingUuid))
                .melding.journalreferanse
            assertThat(journalreferanse).usingRecursiveComparison()
                .isEqualTo(Journalreferanse(JournalpostId("123456"), null))
        }
    }

    @Test
    fun `hentBySaksnummer returnerer alle meldinger for saksnummer`() {
        val saksnummer = "SAK-HENT-001"
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()

        dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            repo.opprettDialogmelding(lagRecord(uuid = uuid1, saksnummer = saksnummer))
            repo.opprettDialogmelding(lagRecord(uuid = uuid2, saksnummer = saksnummer))
            repo.opprettDialogmelding(lagRecord(saksnummer = "ANNEN-SAK"))
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSaksnummer(saksnummer)
        }

        assertEquals(2, resultat.size)
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid1 })
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid2 })
    }

    @Test
    fun `hentBySaksnummer returnerer tom liste for ukjent saksnummer`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSaksnummer("UKJENT-SAK")
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `oppdaterDialogmeldingStatus setter status og tekst`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val statusDto = DialogmeldingStatusDto(
            uuid = record.dialogmeldingUuid.toString(),
            createdAt = OffsetDateTime.now(),
            status = MeldingStatusType.SENDT,
            tekst = "Sendt til behandler",
            bestillingUuid = record.dialogmeldingUuid.toString(),
        )

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).oppdaterDialogmeldingStatus(statusDto)
        }

        val oppdatert = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(MeldingStatusType.SENDT, oppdatert.status)
        assertEquals("Sendt til behandler", oppdatert.statusTekst)
    }

    @Test
    fun `lagreStatusHistorikk lagrer hendelsen kun en gang`() {
        val record = lagRecord()
        val hendelse = DialogmeldingStatusDto(
            uuid = randomUUID().toString(),
            createdAt = OffsetDateTime.parse("2026-10-23T10:00:00+02:00"),
            status = MeldingStatusType.SENDT,
            tekst = "Sendt til behandler",
            bestillingUuid = record.dialogmeldingUuid.toString(),
        )
        val mottattTidspunkt = Instant.parse("2026-10-23T08:01:00Z")

        dataSource.transaction { connection ->
            val repository = DialogmeldingRepositoryImpl(connection)
            repository.opprettDialogmelding(record)
            repository.lagreStatusHistorikk(hendelse, mottattTidspunkt)
            repository.lagreStatusHistorikk(hendelse, mottattTidspunkt.plusSeconds(60))
        }

        val historikk = dataSource.transaction { connection ->
            connection.queryList(
                "SELECT * FROM DIALOGMELDING_STATUS_HISTORIKK WHERE DIALOGMELDING_UUID = ?"
            ) {
                setParams { setUUID(1, record.dialogmeldingUuid) }
                setRowMapper { row ->
                    assertEquals(hendelse.uuid, row.getString("KILDEHENDELSE_ID"))
                    assertEquals(hendelse.status.name, row.getString("STATUS"))
                    assertEquals(hendelse.tekst, row.getString("TEKST"))
                    assertEquals(hendelse.createdAt.toInstant(), row.getInstant("HENDELSE_TIDSPUNKT"))
                    assertEquals(mottattTidspunkt, row.getInstant("MOTTATT_TIDSPUNKT"))
                    row.getUUID("DIALOGMELDING_UUID")
                }
            }
        }

        assertThat(historikk).containsExactly(record.dialogmeldingUuid)
    }

    @Test
    fun `leggTilJournalpostPåBestilling oppdaterer journalpost og dokument id`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection)
                .leggTilJournalpostPåBestilling(record.dialogmeldingUuid, "JP-123", "DOK-456")
        }

        val oppdatert = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals("JP-123", oppdatert.journalpostId)
        assertEquals("DOK-456", oppdatert.dokumentId)
    }

    @Test
    fun `oppdaterFlytStatus setter flytstatus`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection)
                .oppdaterFlytStatus(record.dialogmeldingUuid, ProsesseringSyfoStatus.SENDT_TIL_SYFO)
        }

        val oppdatert = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(ProsesseringSyfoStatus.SENDT_TIL_SYFO, oppdatert.flytStatus)
    }

    @Test
    fun `hentFlytStatus returnerer korrekt flytstatus etter oppdatering`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            repo.opprettDialogmelding(record)
            repo.oppdaterFlytStatus(record.dialogmeldingUuid, ProsesseringSyfoStatus.JOURNALFØRT)
        }

        val flytStatus = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentFlytStatus(record.dialogmeldingUuid)
        }

        assertEquals(record.dialogmeldingUuid, flytStatus.dialogmeldingUuid)
        assertEquals(record.saksnummer, flytStatus.saksnummer)
        assertEquals(ProsesseringSyfoStatus.JOURNALFØRT, flytStatus.flytStatus)
    }

    @Test
    fun `hentFlytStatus har null flytstatus etter opprettelse`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val flytStatus = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentFlytStatus(record.dialogmeldingUuid)
        }

        assertNull(flytStatus.flytStatus)
    }

    @Test
    fun `hentBestillingEldreEnn14Dager returnerer null for ny bestilling`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentBestillingEldreEnn14Dager(record.dialogmeldingUuid)
        }

        assertNull(resultat)
    }

    @Test
    fun `låsBestilling returnerer korrekt uuid`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val låstUuid = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).låsBestilling(record.dialogmeldingUuid)
        }

        assertEquals(record.dialogmeldingUuid, låstUuid)
    }

    @Test
    fun `opprettDialogmelding lagrer tidligereBestillingReferanse`() {
        val tidligereUuid = UUID.randomUUID()
        val record = lagRecord(tidligereBestillingReferanse = tidligereUuid)

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val lagret = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentByDialogId(record.dialogmeldingUuid)
        }!!

        assertEquals(tidligereUuid, lagret.tidligereBestillingReferanse)
    }

    @Test
    fun `hentForParent returnerer melding når parentRef og personIdent matcher`() {
        val record = lagRecord()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(lagRecord())
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(lagRecord())
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForParent(record.dialogmeldingUuid, record.personIdent)
        }

        assertEquals(record.dialogmeldingUuid, resultat?.dialogmeldingUuid)
    }

    @Test
    fun `hentForParent returnerer null for ukjent parentRef`() {
        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(lagRecord())
        }
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForParent(UUID.randomUUID(), randomPersonIdent())
        }

        assertNull(resultat)
    }

    @Test
    fun `hentForParent returnerer null når parentRef tilhører annen person`() {
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()
        val record = lagRecord(personIdent = personA)

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(record)
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForParent(record.dialogmeldingUuid, personB)
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
            val repo = DialogmeldingRepositoryImpl(connection)
            repo.opprettDialogmelding(lagRecord(uuid = uuid1, personIdent = personIdent, samtaleRef = samtaleRef))
            repo.opprettDialogmelding(lagRecord(uuid = uuid2, personIdent = personIdent, samtaleRef = samtaleRef))
            repo.opprettDialogmelding(lagRecord(personIdent = personIdent, samtaleRef = UUID.randomUUID()))
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSamtale(samtaleRef, personIdent)
        }

        assertEquals(2, resultat.size)
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid1 })
        assertTrue(resultat.any { it.dialogmeldingUuid == uuid2 })
    }

    @Test
    fun `hentForSamtale returnerer tom liste for ukjent samtaleRef`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSamtale(UUID.randomUUID(), randomPersonIdent())
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `hentForSamtale returnerer tom liste når samtaleRef tilhører annen person`() {
        val personA = randomPersonIdent()
        val personB = randomPersonIdent()
        val samtaleRef = UUID.randomUUID()

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection)
                .opprettDialogmelding(lagRecord(personIdent = personA, samtaleRef = samtaleRef))
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSamtale(samtaleRef, personB)
        }

        assertTrue(resultat.isEmpty())
    }


    @Test
    fun `hent bestillinger gitt behandlingsreferanse`() {
        val behandlingsreferanse = BehandlingReferanse(UUID.randomUUID())
        val recordLegeerklæring = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40)
        val recordTilleggsopplysning = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L8)


        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(recordLegeerklæring)
            DialogmeldingRepositoryImpl(connection).opprettDialogmelding(recordTilleggsopplysning)
        }

        val bestillinger = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentBestillingerForDokumentasjonstyper(behandlingsreferanse, dokumentasjonstyper = listOf(
                DokumentasjonType.L40, DokumentasjonType.L8))
        }

        assertThat(bestillinger).hasSize(2)
        assertThat(bestillinger.all { it.behandlingsReferanse == behandlingsreferanse.referanse }).isTrue()
        assertThat(bestillinger.map { it.dokumentasjonType }).containsExactlyInAnyOrder(DokumentasjonType.L40, DokumentasjonType.L8)

        val bestillingerBareL40 = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentBestillingerForDokumentasjonstyper(behandlingsreferanse, dokumentasjonstyper = listOf(
                DokumentasjonType.L40))
        }

        assertThat(bestillingerBareL40).hasSize(1)
        assertThat(bestillingerBareL40.first().dokumentasjonType).isEqualTo(DokumentasjonType.L40)

    }

    @Test
    fun `hentForSaksnummer returnerer tom liste når ingen meldinger finnes`() {
        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSaksnummer(randomSaksnummer())
        }

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `hentForSaksnummer filtrerer ut meldinger med et annet saksnummer`() {
        val behandlingsreferanse = BehandlingReferanse(UUID.randomUUID())
        val forsteSaksnummer = randomSaksnummer()
        val melding1 = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = forsteSaksnummer)
        val melding2 = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = randomSaksnummer())
        val melding3 = lagRecord(behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L8, saksnummer = forsteSaksnummer)

        dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            repo.opprettDialogmelding(melding1)
            repo.opprettDialogmelding(melding2)
            repo.opprettDialogmelding(melding3)
        }

        val resultat = dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).hentForSaksnummer(forsteSaksnummer)
        }

        assertThat(resultat.map { it.saksnummer }).containsExactlyInAnyOrder(
            melding1.saksnummer,
            melding3.saksnummer
        )
    }

    @Test
    fun `oppdaterPersonIdentPåSamtaleRef oppdaterer kun gammel ident på alle i samme samtale`() {
        val behandlingsreferanse = BehandlingReferanse(randomUUID())
        val saksnummer = randomSaksnummer()
        val samtaleRef = randomUUID()
        val annenSamtaleRef = randomUUID()
        val nyPersonIdent = randomPersonIdent()
        val gammelIdent = randomPersonIdent()
        val annenPersonIdent = randomPersonIdent()
        val identHistorikk = listOf(nyPersonIdent, gammelIdent)

        val meldingNyIdent = lagRecord(personIdent = nyPersonIdent, samtaleRef = samtaleRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)
        val meldingGammelIdent = lagRecord(personIdent = gammelIdent, samtaleRef = samtaleRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)
        val meldingAnnenPerson = lagRecord(personIdent = annenPersonIdent, samtaleRef = samtaleRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)
        val meldingAnnenSamtale = lagRecord(personIdent = gammelIdent, samtaleRef = annenSamtaleRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)

        val meldingerNyIdentFørEndring = dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            repo.opprettDialogmelding(meldingNyIdent)
            repo.opprettDialogmelding(meldingGammelIdent)
            repo.opprettDialogmelding(meldingAnnenPerson)
            repo.opprettDialogmelding(meldingAnnenSamtale)
            repo.hentForSamtale(samtaleRef, nyPersonIdent)
        }

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).oppdaterPersonIdentPåSamtaleRef(samtaleRef, identHistorikk, nyPersonIdent)
        }

        val (meldingerNyIdentEtterEndring, meldingerNyIdentAnnenSamtale) = dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            val meldingerRiktigSamtale = repo.hentForSamtale(samtaleRef, nyPersonIdent)
            val meldingerAnnenSamtale = repo.hentForSamtale(annenSamtaleRef, nyPersonIdent)
            meldingerRiktigSamtale to meldingerAnnenSamtale
        }

        assertThat(meldingerNyIdentFørEndring.size).isEqualTo(1)
        assertThat(meldingerNyIdentEtterEndring.size).isEqualTo(2)
        assertThat(meldingerNyIdentAnnenSamtale.size).isEqualTo(0)
    }

    @Test
    fun `oppdaterPersonIdentPåParentRef oppdaterer kun gammel ident på parent`() {
        val behandlingsreferanse = BehandlingReferanse(randomUUID())
        val saksnummer = randomSaksnummer()
        val samtaleRef = randomUUID()
        val parentRef = randomUUID()
        val nyPersonIdent = randomPersonIdent()
        val gammelIdent = randomPersonIdent()
        val annenPersonIdent = randomPersonIdent()
        val identHistorikk = listOf(nyPersonIdent, gammelIdent)

        val meldingParent = lagRecord(personIdent = gammelIdent, uuid = parentRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)
        val meldingSamtaleGammelIdent = lagRecord(personIdent = gammelIdent, samtaleRef = samtaleRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)
        val meldingSamtaleAnnenPerson = lagRecord(personIdent = annenPersonIdent, samtaleRef = samtaleRef,
            behandlingsreferanse = behandlingsreferanse.referanse, dokumentasjonType = DokumentasjonType.L40, saksnummer = saksnummer)

        val meldingerParentNyIdentFørEndring = dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            repo.opprettDialogmelding(meldingParent)
            repo.opprettDialogmelding(meldingSamtaleGammelIdent)
            repo.opprettDialogmelding(meldingSamtaleAnnenPerson)
            repo.hentForParent(parentRef, nyPersonIdent)
        }

        dataSource.transaction { connection ->
            DialogmeldingRepositoryImpl(connection).oppdaterPersonIdentPåParentRef(parentRef, identHistorikk, nyPersonIdent)
        }

        val (meldingerParentNyIdentEtterEndring, meldingerSamtaleNyIdentEtterEndring) = dataSource.transaction { connection ->
            val repo = DialogmeldingRepositoryImpl(connection)
            val meldingerParent = repo.hentForParent(parentRef, nyPersonIdent)
            val meldingerSamtale = repo.hentForSamtale(samtaleRef, nyPersonIdent)
            meldingerParent to meldingerSamtale
        }

        assertThat(meldingerParentNyIdentFørEndring == null)
        assertThat(meldingerParentNyIdentEtterEndring != null)
        assertThat(meldingerSamtaleNyIdentEtterEndring.size).isEqualTo(0)
    }
}
