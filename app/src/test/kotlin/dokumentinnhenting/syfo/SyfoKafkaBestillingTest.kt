package dokumentinnhenting.syfo

import dokumentinnhenting.integrasjoner.syfo.bestilling.*
import dokumentinnhenting.integrasjoner.syfo.status.*
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase

import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class SyfoKafkaBestillingTest {
    private lateinit var behandlerDialogmeldingBestilling: BehandlerDialogmeldingBestilling
    private val mockProducer = mockk<KafkaProducer<String, DialogmeldingToBehandlerBestillingDTO>>(relaxed = true)
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

    @BeforeEach
    fun setup() {
        InitTestDatabase.migrate()
        val dataSource = InitTestDatabase.dataSource
        val transactionProvider = TransactionProvider(dataSource)

        behandlerDialogmeldingBestilling = BehandlerDialogmeldingBestilling(
            monitor = mockk(relaxed = true),
            datasource = dataSource,
            transactionProvider = transactionProvider,
            producer = mockProducer
        )
    }

    @AfterEach
    fun afterEach() {
        InitTestDatabase.clean()
    }

    @Test
    fun kanSendeBestilling() {
        val sakId = "sakId"
        val dto = BehandlingsflytToDialogmeldingDTO(
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            sakId = sakId,
            dialogmeldingType = "DIALOG_NOTAT",
            dialogmeldingKodeverk = "HENVENDELSE",
            dialogmeldingKode = 1234,
            dialogmeldingTekst = "tekst",
            dialogmeldingVedlegg = null
        )

        val dialogmeldingUuid = behandlerDialogmeldingBestilling.dialogmeldingBestilling(dto)

        verify(exactly = 1) {
            mockProducer.send(withArg { record: ProducerRecord<String, DialogmeldingToBehandlerBestillingDTO> ->
                assert(record.topic() == SYFO_BESTILLING_DIALOGMELDING_TOPIC)
                assert(record.key() == dialogmeldingUuid)
            })
        }

        val lagretBestilling = hentRepositoryData(sakId)
        assertEquals(UUID.fromString(dialogmeldingUuid), lagretBestilling[0].dialogmeldingUuid)
    }

    private fun hentRepositoryData(sakId: String): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        return InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySakId(sakId)
        }
    }
}
