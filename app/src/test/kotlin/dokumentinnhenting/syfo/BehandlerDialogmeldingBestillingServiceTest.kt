package dokumentinnhenting.syfo

import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.WithFakes
import dokumentinnhenting.api.tilDto
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestillingService
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingBrevGeneratorService
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.syfosteg.BestillLegeerklæringSteg
import dokumentinnhenting.util.motor.syfo.syfosteg.SYFO_BESTILLING_DIALOGMELDING_TOPIC
import dokumentinnhenting.util.motor.syfo.syfosteg.SyfoSteg
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.random.Random

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlerDialogmeldingBestillingServiceTest {
    private lateinit var behandlerDialogmeldingBestillingService: BehandlerDialogmeldingBestillingService
    private val dialogmeldingBrevGeneratorService = mockk<DialogmeldingBrevGeneratorService>(relaxed = true)
    private val mockProducer = mockk<KafkaProducer<String, String>>(relaxed = true)
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

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
        val dto = BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = UUID.randomUUID(),
            behandlerHprNr = "12344321"
        )

        lateinit var dialogmeldingUuid: UUID

        dataSource.transaction { connection ->
            //Første del, lagring av dialogmelding i repository
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection
            )

            //Andre del, henter data i steg og sender til kafka
            dialogmeldingUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(dto)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(dialogmeldingUuid, "journalpostid", "dokumentid")
            val azureTokenGen = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")
            azureTokenGen.generate()
            val steg =
                BestillLegeerklæringSteg(dialogmeldingRepository, dialogmeldingBrevGeneratorService, mockProducer)
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

    @Test
    fun `feiler om purring mangler tilhørende legeerklæring`() {
        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            assertThrows<RuntimeException> {
                behandlerDialogmeldingBestillingService.dialogmeldingPurring(
                    LegeerklæringPurringDto(UUID.randomUUID())
                )
            }
        }
    }

    @Test
    fun `feiler om man prøver å purre på legeerklæring som er under 14 dager gammel`() {
        lateinit var dialogmeldingLegerklæringUuid: UUID
        val saksnummer = Random.nextLong().toString()
        val legeerklæring = BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = UUID.randomUUID(),
            behandlerHprNr = "1233321"
        )

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection = connection
            )

            dialogmeldingLegerklæringUuid =
                behandlerDialogmeldingBestillingService.dialogmeldingBestilling(legeerklæring)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(
                dialogmeldingLegerklæringUuid,
                "journalpostid",
                "dokumentid"
            )
        }

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection = connection
            )

            assertThrows<RuntimeException> {
                behandlerDialogmeldingBestillingService.dialogmeldingPurring(
                    LegeerklæringPurringDto(dialogmeldingLegerklæringUuid)
                )
            }
        }
    }

    @Test
    fun `sender automatisk purring når bestilling er 3 uker og 1 dag gammel`() {
        val behandlingsReferanse = UUID.randomUUID()
        val saksnummer = Random.nextLong().toString()
        val legeerklæring = BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = behandlingsReferanse,
            behandlerHprNr = "12344321"
        )

        lateinit var dialogmeldingUuid: UUID

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            dialogmeldingUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(legeerklæring)
        }

        // Setter opprettet tid til tre uker og en dag siden
        val treUkerOgEnDagSiden = LocalDate.now().minusWeeks(3).minusDays(1).atStartOfDay()
        oppdaterOpprettetTidspunktForDialogmeldingRecord(treUkerOgEnDagSiden, dialogmeldingUuid)

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            behandlerDialogmeldingBestillingService.sendAutomatiskPurringHvisBestillingFinnes(
                BehandlingReferanse(behandlingsReferanse)
            )

            val lagretBestillinger = dialogmeldingRepository.hentBySaksnummer(saksnummer)

            val purring = lagretBestillinger.find {
                it.dokumentasjonType == DokumentasjonType.PURRING
            }
            assertThat(purring).isNotNull
            assertThat(purring!!.tidligereBestillingReferanse).isEqualTo(dialogmeldingUuid)
        }
    }

    @Test
    fun `sender ikke automatisk purring når bestilling allerede har purring`() {
        val behandlingsReferanse = UUID.randomUUID()
        val saksnummer = Random.nextLong().toString()
        val legeerklæring = BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L40,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = behandlingsReferanse,
            behandlerHprNr = "12344321"
        )

        lateinit var dialogmeldingUuid: UUID

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            dialogmeldingUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(legeerklæring)
        }

        // Setter opprettet-tid tre uker og en dag tilbake i tid
        val treUkerOgEnDagSiden = LocalDate.now().minusWeeks(3).minusDays(1).atStartOfDay()
        oppdaterOpprettetTidspunktForDialogmeldingRecord(treUkerOgEnDagSiden, dialogmeldingUuid)


        dataSource.transaction { connection ->
            // lager manuell purring
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)
            behandlerDialogmeldingBestillingService.dialogmeldingPurring(
                dto = LegeerklæringPurringDto(dialogmeldingUuid)
            )

            val purringerFørAutomatisk = dialogmeldingRepository.hentBySaksnummer(saksnummer).filter { it.dokumentasjonType == DokumentasjonType.PURRING }
            assertThat(purringerFørAutomatisk).hasSize(1)

            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            behandlerDialogmeldingBestillingService.sendAutomatiskPurringHvisBestillingFinnes(
                BehandlingReferanse(behandlingsReferanse)
            )

            // skal ikke opprette ny purring hvis det allerede finnes en
            val lagretBestillinger = dialogmeldingRepository.hentBySaksnummer(saksnummer)
            val purringerEtterAutomatisk = lagretBestillinger.filter {
                it.dokumentasjonType == DokumentasjonType.PURRING
            }
            assertThat(purringerEtterAutomatisk.size).isEqualTo(1)
            assertThat(purringerEtterAutomatisk.first().tidligereBestillingReferanse).isEqualTo(dialogmeldingUuid)
        }
    }

    private fun hentRepositoryData(
        dataSource: DataSource,
        saksnummer: String
    ): List<DialogmeldingStatusTilBehandslingsflytDto> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
                .map(DialogmeldingFullRecord::tilDto)
        }
    }

    private fun oppdaterOpprettetTidspunktForDialogmeldingRecord(tidspunkt: LocalDateTime, dialogmeldingUuid: UUID) {
        dataSource.transaction { connection ->
            connection.execute(
                """UPDATE DIALOGMELDING SET OPPRETTET_TID = ? WHERE DIALOGMELDING_UUID = ?""".trimIndent()
            ) {
                setParams {
                    setLocalDateTime(1, tidspunkt)
                    setUUID(2, dialogmeldingUuid)
                }
            }
        }
    }
}
