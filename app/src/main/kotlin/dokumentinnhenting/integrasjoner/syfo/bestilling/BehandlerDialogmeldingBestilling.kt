package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.syfo.status.TransactionProvider
import dokumentinnhenting.util.kafka.config.ProducerConfig
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
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource),
    private val producer: KafkaProducer<String, DialogmeldingToBehandlerBestillingDTO> = KafkaProducer(ProducerConfig().properties())
) {
    init {
        monitor.subscribe(ApplicationStopped) {
            producer.close()
        }
    }

    fun dialogmeldingBestilling(dto: BehandlingsflytToDialogmeldingDTO): String {
        val mappedBestilling = mapToDialogMeldingBestilling(dto)
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
        val brevTekst = genererBrev(dto)
        val kodeStruktur = mapDialogmeldingKodeStruktur(dto.dokumentasjonType)
        return DialogmeldingToBehandlerBestillingDTO(
            dto.behandlerRef,
            dto.personIdent,
            UUID.randomUUID().toString(),
            null, // Trenger vi denne?
            UUID.randomUUID().toString(), // Trenger vi denne?
            kodeStruktur.dialogmeldingType,
            kodeStruktur.dialogmeldingKodeverk,
            kodeStruktur.dialogmeldingKode,
            brevTekst,
            dto.dialogmeldingVedlegg
        )
    }

    private fun mapDialogmeldingKodeStruktur(dokumentasjonType: DokumentasjonType): DialogmeldingKodeStruktur {
        return when (dokumentasjonType) {
            DokumentasjonType.L40 -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL, 1)
            DokumentasjonType.L8 -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL, 1)
            DokumentasjonType.MELDING_FRA_NAV -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_NOTAT, DialogmeldingKodeverk.HENVENDELSE, 8)
            DokumentasjonType.RETUR_LEGEERKLÆRING -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_NOTAT, DialogmeldingKodeverk.HENVENDELSE, 3)
            DokumentasjonType.L120 -> TODO() // TODO: Neste fase, lage brev og mapping for 120
        }
    }

    private fun skrivDialogmeldingTilRepository(melding: DialogmeldingRecord) {
        log.info("Mottatt dialogmelding-bestilling på sak ${melding.sakId}")
        transactionProvider.inTransaction {
            dialogmeldingRepository.opprettDialogmelding(melding)
        }
    }

    private data class DialogmeldingKodeStruktur(
        val dialogmeldingType: DialogmeldingType,
        val dialogmeldingKodeverk: DialogmeldingKodeverk,
        val dialogmeldingKode: Int
    )
}