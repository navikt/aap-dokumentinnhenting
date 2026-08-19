package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.WithFakes
import dokumentinnhenting.api.tilDto
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.syfosteg.BestillLegeerklæringSteg
import dokumentinnhenting.util.motor.syfo.syfosteg.SYFO_BESTILLING_DIALOGMELDING_TOPIC
import dokumentinnhenting.util.motor.syfo.syfosteg.SyfoSteg
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import java.util.UUID.randomUUID
import javax.sql.DataSource
import kotlin.random.Random

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BestillLegeerklæringStegTest {
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
            behandlingsReferanse = randomUUID(),
            behandlerHprNr = "12344321"
        )

        lateinit var dialogmeldingUuid: UUID

        dataSource.transaction { connection ->
            // Første del, lagring av dialogmelding i repository
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection
            )

            // Andre del, henter data i steg og sender til kafka
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

    private fun hentRepositoryData(
        dataSource: DataSource,
        saksnummer: String
    ): List<DialogmeldingStatusTilBehandslingsflytDto> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentForSaksnummer(saksnummer)
                .map(DialogmeldingFullRecord::tilDto)
        }
    }
}