package integrasjonportal.integrasjoner.syfo.bestilling

import integrasjonportal.repositories.DialogmeldingRepository
import integrasjonportal.util.kafka.config.ProducerConfig
import io.ktor.events.*
import io.ktor.server.application.*
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import java.util.*

private const val SYFO_BESTILLING_DIALOGMELDING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
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

    //TODO: Få noe retur på dette slik at behandlingsflyt har litt peiling på hva som skjer?
    fun dialogmeldingBestilling(dto: BehandlingsflytToDialogmeldingDTO) {
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

        } catch (e: Exception) {
            log.error("Feilet ved sending til topic $SYFO_BESTILLING_DIALOGMELDING_TOPIC", e)
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

    class TransactionContext(
        val dialogmeldingRepository: DialogmeldingRepository
    )

    class TransactionProvider(
        val datasource: DataSource
    ) {
        //Todo: jobb-implementasjon i block her?
        fun inTransaction(block: TransactionContext.() -> Unit) {
            datasource.transaction {
                TransactionContext(DialogmeldingRepository(it))
            }
        }
    }

    private fun skrivDialogmeldingTilRepository(melding: DialogmeldingRecord) {
        log.info("Mottatt dialogmelding-bestilling på sak ${melding.sakId}")
        transactionProvider.inTransaction {
            dialogmeldingRepository.opprettDialogmelding(melding)
        }
    }
}