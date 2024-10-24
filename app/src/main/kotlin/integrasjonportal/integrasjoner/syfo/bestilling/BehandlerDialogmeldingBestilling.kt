package integrasjonportal.integrasjoner.syfo.bestilling

import integrasjonportal.integrasjoner.syfo.status.TransactionProvider
import integrasjonportal.util.kafka.config.ProducerConfig
import io.ktor.events.*
import io.ktor.server.application.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import java.util.*

const val SYFO_BESTILLING_DIALOGMELDING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
private val log = LoggerFactory.getLogger(BehandlerDialogmeldingBestilling::class.java)

class BehandlerDialogmeldingBestilling(
    monitor: Events,
    datasource: DataSource,
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource)
) {
    private val producer = KafkaProducer<String, DialogmeldingToBehandlerBestillingDTO>(ProducerConfig().properties())

    init {
        monitor.subscribe(ApplicationStopped) {
            producer.close()
        }
    }

    fun dialogmeldingBestilling(dto: BehandlingsflytToDialogmeldingDTO): String {
        val mappedBestilling = mapToDialogMeldingBestilling(dto)
        val config = ProducerConfig().properties()
        val producer = KafkaProducer<String, DialogmeldingToBehandlerBestillingDTO>(config)
        val record = ProducerRecord(SYFO_BESTILLING_DIALOGMELDING_TOPIC, mappedBestilling.dialogmeldingUuid, mappedBestilling)

        try {
            producer.send(record).get()

            // TODO: flytt til jobb i motor
            skrivDialogmeldingTilRepository(
                DialogmeldingRecord(UUID.fromString(mappedBestilling.dialogmeldingUuid), dto.behandlerRef, dto.personIdent, dto.sakId)
            )

            return mappedBestilling.dialogmeldingUuid
        } catch (e: Exception) {
            log.error("Feilet ved sending til topic $SYFO_BESTILLING_DIALOGMELDING_TOPIC", e)
            return e.toString()
        }
    }

    private fun mapToDialogMeldingBestilling(dto: BehandlingsflytToDialogmeldingDTO): DialogmeldingToBehandlerBestillingDTO {
        return DialogmeldingToBehandlerBestillingDTO(
            dto.behandlerRef,
            dto.personIdent,
            UUID.randomUUID().toString(),
            null, // Trenger vi denne?
            UUID.randomUUID().toString(), // Trenger vi denne?
            dto.dialogmeldingType, // "DIALOG_NOTAT"?,
            dto.dialogmeldingKodeverk, // "HENVENDELSE"?,
            dto.dialogmeldingKode,
            dto.dialogmeldingTekst,
            dto.dialogmeldingVedlegg
        )
    }

    private fun skrivDialogmeldingTilRepository(melding: DialogmeldingRecord) {
        log.info("Mottatt dialogmelding-bestilling på sak ${melding.sakId}")
        transactionProvider.inTransaction {
            dialogmeldingRepository.opprettDialogmelding(melding)
        }
    }
}