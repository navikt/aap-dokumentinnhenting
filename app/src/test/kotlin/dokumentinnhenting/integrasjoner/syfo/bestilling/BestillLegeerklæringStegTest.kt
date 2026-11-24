package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.WithFakes
import dokumentinnhenting.api.tilDto
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.PåminnelseHistorikkRepositoryImpl
import dokumentinnhenting.repositories.PåminnelseRepositoryImpl
import dokumentinnhenting.unleash.FakeUnleashGateway
import dokumentinnhenting.util.motor.syfo.syfosteg.BestillLegeerklæringSteg
import dokumentinnhenting.util.motor.syfo.syfosteg.SYFO_BESTILLING_DIALOGMELDING_TOPIC
import dokumentinnhenting.util.motor.syfo.syfosteg.SyfoSteg
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource
import kotlin.random.Random

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BestillLegeerklæringStegTest {
    private lateinit var behandlerDialogmeldingBestillingService: BehandlerDialogmeldingBestillingService
    private val dialogmeldingBrevGeneratorService = mockk<DialogmeldingBrevGeneratorService>(relaxed = true)
    private val mockProducer = mockk<KafkaProducer<String, String>>(relaxed = true)
    private lateinit var dialogmeldingRepository: DialogmeldingRepositoryImpl
    private lateinit var påminnelseRepository: PåminnelseRepositoryImpl
    private lateinit var påminnelseHistorikkRepository: PåminnelseHistorikkRepositoryImpl

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
    fun `kan kjøre steg`() {
        val saksnummer = Random.nextLong().toString()
        val dto = lagDokumentinnhentingBestilling(saksnummer, DokumentasjonType.L8)

        lateinit var dialogmeldingUuid: UUID

        dataSource.transaction { connection ->
            // Første del, lagring av dialogmelding i repository
            dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection,
                FakeUnleashGateway(enabled = true)
            )
            påminnelseRepository = PåminnelseRepositoryImpl(connection)
            påminnelseHistorikkRepository = PåminnelseHistorikkRepositoryImpl(connection)

            // Andre del, henter data i steg og sender til kafka
            dialogmeldingUuid = behandlerDialogmeldingBestillingService.bestillNyDialogmeldingForespørsel(dto)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(dialogmeldingUuid, "journalpostid", "dokumentid")
            val azureTokenGen = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")
            azureTokenGen.generate()
            val steg =
                BestillLegeerklæringSteg(
                    dialogmeldingRepository,
                    dialogmeldingBrevGeneratorService,
                    mockProducer,
                    påminnelseRepository,
                    påminnelseHistorikkRepository,
                    FakeUnleashGateway(enabled = true)
                )
            steg.utfør(SyfoSteg.Kontekst(dialogmeldingUuid))
        }

        verify(exactly = 1) {
            mockProducer.send(withArg { record: ProducerRecord<String, String> ->
                assert(record.topic() == SYFO_BESTILLING_DIALOGMELDING_TOPIC)
                assert(record.key() == dialogmeldingUuid.toString())
            })
        }

        val lagretBestilling = hentRepositoryData(dataSource, saksnummer)
        assertEquals(dialogmeldingUuid, lagretBestilling[0].dialogmeldingUuid)
    }

    @org.junit.jupiter.api.BeforeEach
    fun resetProducer() {
        io.mockk.clearMocks(mockProducer)
    }

    @Test
    fun `Status i Påminnelse og PåminnelseHistorikk forblir PLANLAGT ved L40 forespørsel som ikke er PURRING`() {
        val saksnummer = Random.nextLong().toString()
        val dto = lagDokumentinnhentingBestilling(saksnummer, DokumentasjonType.L40)

        every { mockProducer.send(any<ProducerRecord<String, String>>()) } returns
                CompletableFuture.completedFuture(mockk<RecordMetadata>())

        lateinit var dialogmeldingUuid: UUID
        lateinit var påminnelse: PåminnelseRecord
        lateinit var påminnelseHistorikk: PåminnelseHistorikkRecord

        dataSource.transaction { connection ->
            settRepositories(connection)

            dialogmeldingUuid = behandlerDialogmeldingBestillingService.bestillNyDialogmeldingForespørsel(
                dto
            )
            dialogmeldingRepository.leggTilJournalpostPåBestilling(
                dialogmeldingUuid,
                "journalpostid",
                "dokumentid"
            )

            val azureTokenGen = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")
            azureTokenGen.generate()

            val steg = BestillLegeerklæringSteg(
                dialogmeldingRepository,
                dialogmeldingBrevGeneratorService,
                mockProducer,
                påminnelseRepository,
                påminnelseHistorikkRepository,
                FakeUnleashGateway(enabled = true)
            )

            steg.utfør(SyfoSteg.Kontekst(dialogmeldingUuid))

            assertEquals(
                SyfoSteg.Resultat.FULLFØRT,
                steg.utfør(SyfoSteg.Kontekst(dialogmeldingUuid))
            )

            påminnelse = påminnelseRepository.hentPåminnelseForDialogmeldingId(dialogmeldingUuid).first()

            assertThat(påminnelse).isNotNull

            påminnelseHistorikk = påminnelseHistorikkRepository.hentPåminnelserPåPåminnelseId(påminnelse.id!!).last()
        }

        val lagretBestilling = hentRepositoryData(dataSource, saksnummer)
        assertEquals(dialogmeldingUuid, lagretBestilling[0].dialogmeldingUuid)

        assertThat(påminnelse.status).isEqualTo(PåminnelseStatus.PLANLAGT)
        assertThat(påminnelse.sisteForespørselSendt).isNotNull

        assertThat(påminnelseHistorikk.påminnelseStatus).isEqualTo(PåminnelseStatus.PLANLAGT)
        assertThat(påminnelseHistorikk.sisteForespørselSendt).isNotNull
    }

    @Test
    fun `Status i Påminnelse og PåminnelseHistorikk blir satt til SENDT ved PURRING`() {
        val saksnummer = Random.nextLong().toString()

        lateinit var opprinneligDialogmeldingUuid: UUID
        lateinit var påminnelse: PåminnelseRecord
        lateinit var påminnelseHistorikk: PåminnelseHistorikkRecord

        every { mockProducer.send(any<ProducerRecord<String, String>>()) } returns
                CompletableFuture.completedFuture(mockk<RecordMetadata>())

        dataSource.transaction { connection ->
            // Felles for opprinnelig forespørsel og påminnelse
            settRepositories(connection)

            val azureTokenGen = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")
            azureTokenGen.generate()

            val steg = BestillLegeerklæringSteg(
                dialogmeldingRepository,
                dialogmeldingBrevGeneratorService,
                mockProducer,
                påminnelseRepository,
                påminnelseHistorikkRepository,
                FakeUnleashGateway(enabled = true)
            )

            // Send opprinnelig L40-forespørsel
            val dtoOpprinneligForespørsel = lagDokumentinnhentingBestilling(
                saksnummer,
                DokumentasjonType.L40
            )
            opprinneligDialogmeldingUuid = behandlerDialogmeldingBestillingService.bestillNyDialogmeldingForespørsel(
                dtoOpprinneligForespørsel
            )
            dialogmeldingRepository.leggTilJournalpostPåBestilling(opprinneligDialogmeldingUuid, "journalpostid", "dokumentid")

            val syfoResultatForespørsel = steg.utfør(SyfoSteg.Kontekst(opprinneligDialogmeldingUuid))

            assertEquals(
                SyfoSteg.Resultat.FULLFØRT,
                syfoResultatForespørsel
            )

            // Send påminnelse
            val dtoPåminnelse = lagDokumentinnhentingBestilling(
                saksnummer,
                DokumentasjonType.PURRING,
                tidligereBestillingReferanse = opprinneligDialogmeldingUuid
            )
            val påminnelseDialogmeldingUuid = behandlerDialogmeldingBestillingService.bestillDialogmeldingPåminnelse(
                dtoPåminnelse,
                randomUUID(),
            )
            dialogmeldingRepository.leggTilJournalpostPåBestilling(
                påminnelseDialogmeldingUuid,
                "journalpostid",
                "dokumentid"
            )

            val syfoStatusPåminnelse = steg.utfør(SyfoSteg.Kontekst(påminnelseDialogmeldingUuid))

            assertEquals(
                SyfoSteg.Resultat.FULLFØRT,
                syfoStatusPåminnelse
            )

            // Hent resultat
            påminnelse = påminnelseRepository.hentPåminnelseForDialogmeldingId(opprinneligDialogmeldingUuid).first()

            assertThat(påminnelse).isNotNull

            påminnelseHistorikk = påminnelseHistorikkRepository.hentPåminnelserPåPåminnelseId(påminnelse.id!!).last()
        }

        val lagretBestilling = hentRepositoryData(dataSource, saksnummer)
        assertEquals(opprinneligDialogmeldingUuid, lagretBestilling[0].dialogmeldingUuid)

        assertThat(påminnelse.status).isEqualTo(PåminnelseStatus.SENDT)
        assertThat(påminnelse.sisteForespørselSendt).isNotNull

        assertThat(påminnelseHistorikk.påminnelseStatus).isEqualTo(PåminnelseStatus.SENDT)
        assertThat(påminnelseHistorikk.sisteForespørselSendt).isNotNull
    }

    private fun settRepositories(connection: DBConnection) {
        // Første del, lagring av dialogmelding i repository
        dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
        behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
            connection,
            FakeUnleashGateway(enabled = true)
        )
        påminnelseRepository = PåminnelseRepositoryImpl(connection)
        påminnelseHistorikkRepository = PåminnelseHistorikkRepositoryImpl(connection)
    }

    private fun hentRepositoryData(
        dataSource: DataSource,
        saksnummer: String
    ): List<DialogmeldingStatusTilBehandslingsflytDto> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            dialogmeldingRepository.hentForSaksnummer(saksnummer)
                .map(DialogmeldingFullRecord::tilDto)
        }
    }

    private fun lagDokumentinnhentingBestilling(
        saksnummer: String,
        dokumentasjonsType: DokumentasjonType,
        tidligereBestillingReferanse: UUID? = null,
    ): BehandlingsflytToDokumentInnhentingBestillingDto {
        return BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = dokumentasjonsType,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = randomUUID(),
            behandlerHprNr = "12344321",
            tidligereBestillingReferanse = tidligereBestillingReferanse,
        )
    }
}