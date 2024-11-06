package dokumentinnhenting.syfo

import dokumentinnhenting.integrasjoner.syfo.bestilling.*
import dokumentinnhenting.integrasjoner.syfo.status.*
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.syfosteg.SYFO_BESTILLING_DIALOGMELDING_TOPIC
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase

import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import javax.sql.DataSource

class SyfoKafkaBestillingTest {
    private lateinit var behandlerDialogmeldingBestillingService: BehandlerDialogmeldingBestillingService
    private val mockProducer = mockk<KafkaProducer<String, DialogmeldingToBehandlerBestillingDTO>>(relaxed = true)
    private lateinit var dialogmeldingRepository: DialogmeldingRepository
    private lateinit var jobbRepository: FlytJobbRepository

    @BeforeEach
    fun setup() {
        InitTestDatabase.migrate()
    }

    @AfterEach
    fun afterEach() {
        InitTestDatabase.clean()
    }

    @Test
    fun kanSendeBestilling() {
        val saksnummer = "saksnummer"
        val dto = BehandlingsflytToDialogmeldingDTO(
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dialogmeldingVedlegg = null,
            dokumentasjonType = DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            veilederNavn = "veilederNavn"
        )

        lateinit var dialogmeldingUuid: UUID

        InitTestDatabase.dataSource.transaction { connection ->
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(FlytJobbRepository(connection), DialogmeldingRepository(connection))
            dialogmeldingUuid = behandlerDialogmeldingBestillingService.dialogmeldingBestilling(dto)
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

    private fun hentRepositoryData(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        return InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
        }
    }
}
