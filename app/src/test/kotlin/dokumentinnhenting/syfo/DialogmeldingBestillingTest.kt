package dokumentinnhenting.syfo

import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.Fakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.*
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.syfosteg.BestillLegeerklæringSteg
import dokumentinnhenting.util.motor.syfo.syfosteg.SYFO_BESTILLING_DIALOGMELDING_TOPIC
import dokumentinnhenting.util.motor.syfo.syfosteg.SyfoSteg
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class DialogmeldingBestillingTest {
    private lateinit var behandlerDialogmeldingBestillingService: BehandlerDialogmeldingBestillingService
    private val mockProducer = mockk<KafkaProducer<String, String>>(relaxed = true)
    private lateinit var dialogmeldingRepository: DialogmeldingRepository
    val fakes = Fakes

    @BeforeEach
    fun setup() {
        InitTestDatabase.migrate()
    }

    @AfterEach
    fun afterEach() {
        InitTestDatabase.clean()
    }

    @Test
    fun kanKjøreSteg() {
            val saksnummer = "saksnummer"
            val dto = BehandlingsflytToDokumentInnhentingBestillingDTO(
                bestillerNavIdent = "bestillerNavIdent",
                behandlerRef = "behandlerRef",
                personIdent = "12345678910",
                personNavn = "personNavn",
                saksnummer = saksnummer,
                dialogmeldingTekst = "tekst",
                dokumentasjonType = DokumentasjonType.L8,
                behandlerNavn = "behandlerNavn",
                behandlingsReferanse = UUID.randomUUID(),
                behandlerHprNr = "12344321"
            )

            lateinit var dialogmeldingUuid: UUID

            InitTestDatabase.dataSource.transaction { connection ->
                //Første del, lagring av dialogmelding i repository
                dialogmeldingRepository = DialogmeldingRepository(connection)
                behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                    FlytJobbRepository(connection),
                    DialogmeldingRepository(connection)
                )

                //Andre del, henter data i steg og sender til kafka
                dialogmeldingUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(dto)
                dialogmeldingRepository.leggTilJournalpostPåBestilling(dialogmeldingUuid, "journalpostid", "dokumentid")
                val azureTokenGen = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")
                azureTokenGen.generate()
                val steg = BestillLegeerklæringSteg(dialogmeldingRepository, mockProducer)
                steg.utfør(SyfoSteg.Kontekst(dialogmeldingUuid))
            }

            verify(exactly = 1) {
                mockProducer.send(withArg { record: ProducerRecord<String, DialogmeldingToBehandlerBestillingDTO> ->
                    assert(record.topic() == SYFO_BESTILLING_DIALOGMELDING_TOPIC)
                    assert(record.key() == dialogmeldingUuid.toString())
                })
            }

            val lagretBestilling = hentRepositoryData(saksnummer)
            assertEquals(dialogmeldingUuid, lagretBestilling[0].dialogmeldingUuid)
    }

    @Test
    fun FeilerOmPurringManglerTilhørendeLegeerklæring() {
        InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(FlytJobbRepository(connection), DialogmeldingRepository(connection))

            assertThrows<RuntimeException> { behandlerDialogmeldingBestillingService.dialogmeldingPurring(LegeerklæringPurringDTO(UUID.randomUUID()))}
        }
    }

    @Test
    fun KanOppdatereBestillingStatuserManuelt() {
        val saksnummer = "saksnummer"
        val dto = BehandlingsflytToDokumentInnhentingBestillingDTO(
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = UUID.randomUUID(),
            behandlerHprNr = "12344321"
        )

        lateinit var dialogmeldingUuid: UUID

        InitTestDatabase.dataSource.transaction { connection ->
            //Første del, lagring av dialogmelding i repository
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                FlytJobbRepository(connection),
                DialogmeldingRepository(connection)
            )

            //Andre del, henter data i steg og sender til kafka
            dialogmeldingUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(dto)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(dialogmeldingUuid, "journalpostid", "dokumentid")
            val azureTokenGen = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting")
            azureTokenGen.generate()
            val steg = BestillLegeerklæringSteg(dialogmeldingRepository, mockProducer)
            steg.utfør(SyfoSteg.Kontekst(dialogmeldingUuid))
        }

        val lagretBestilling = hentRepositoryDataByDialogId(dialogmeldingUuid)
        assertEquals(null, lagretBestilling.status)

        InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.oppdaterDialogmeldingStatusMedMottatt(dialogmeldingUuid)
        }

        val oppdatertBestilling = hentRepositoryDataByDialogId(dialogmeldingUuid)
        assertEquals(MeldingStatusType.MOTTATT, oppdatertBestilling.status)
    }

    @Test
    fun FeilerOmLegeerklæringPurringErUnder14Dager() {
        lateinit var dialogmeldingLegerklæringUuid: UUID
        val saksnummer = "saksnummer"
        val legeerklæring = BehandlingsflytToDokumentInnhentingBestillingDTO(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = UUID.randomUUID(),
            behandlerHprNr = "1233321"
        )

        InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                FlytJobbRepository(connection),
                DialogmeldingRepository(connection)
            )

            dialogmeldingLegerklæringUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(legeerklæring)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(
                dialogmeldingLegerklæringUuid,
                "journalpostid",
                "dokumentid"
            )
        }

        InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                FlytJobbRepository(connection),
                DialogmeldingRepository(connection)
            )

            assertThrows<RuntimeException> {
                behandlerDialogmeldingBestillingService.dialogmeldingPurring(
                    LegeerklæringPurringDTO(dialogmeldingLegerklæringUuid)
                )
            }
        }
    }

    private fun hentRepositoryData(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        return InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
        }
    }

    private fun hentRepositoryDataByDialogId(dialogmeldingId: UUID): DialogmeldingFullRecord {
        return InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentByDialogId(dialogmeldingId)!!
        }
    }
}
