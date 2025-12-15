package dokumentinnhenting.util.motor.syfo.syfosteg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dokumentinnhenting.integrasjoner.saf.SafRestGateway
import dokumentinnhenting.integrasjoner.syfo.bestilling.*
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.kafka.config.ProducerConfig
import dokumentinnhenting.util.metrics.bestillingCounter
import dokumentinnhenting.util.metrics.prometheus
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*
import kotlinx.coroutines.runBlocking

const val SYFO_BESTILLING_DIALOGMELDING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
const val KILDE = "AAP"

class BestillLegeerklæringSteg(
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val producer: KafkaProducer<String, String> = KafkaProducer(ProducerConfig().properties())

): SyfoSteg.Utfører {
    private val objectMapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(StartLegeerklæringBestillingSteg::class.java)

    override fun utfør(kontekst: SyfoSteg.Kontekst): SyfoSteg.Resultat {
        log.info("BestillLegeerklæringSteg")
        return sendBestilling(kontekst.referanse)
    }

    companion object : SyfoSteg {
        override fun konstruer(connection: DBConnection): SyfoSteg.Utfører {
            return BestillLegeerklæringSteg(DialogmeldingRepository(connection))
        }
    }

    fun sendBestilling(dialogmeldingUuid: UUID): SyfoSteg.Resultat {
        val funnetBestilling = requireNotNull(dialogmeldingRepository.hentByDialogId(dialogmeldingUuid))
        val mappedBestilling = mapToDialogMeldingBestilling(dialogmeldingUuid, funnetBestilling)

        val jsonValue = objectMapper.writeValueAsString(mappedBestilling)

        val record = ProducerRecord(SYFO_BESTILLING_DIALOGMELDING_TOPIC, mappedBestilling.dialogmeldingUuid.toString(), jsonValue)

        try {
            producer.send(record).get()
        } catch (e: Exception) {
            log.error("Feilet ved sending til topic $SYFO_BESTILLING_DIALOGMELDING_TOPIC", e)
            return SyfoSteg.Resultat.STOPP
        }
        prometheus.bestillingCounter(SYFO_BESTILLING_DIALOGMELDING_TOPIC).increment()

        return SyfoSteg.Resultat.FULLFØRT
    }

    private fun mapToDialogMeldingBestilling(dialogmeldingUuid: UUID, record: DialogmeldingFullRecord): DialogmeldingToBehandlerBestillingDTO {
        val pdfBrev: ByteArray = runBlocking {
            SafRestGateway.hentDokumentMedJournalpostId(
                requireNotNull(record.journalpostId), requireNotNull(record.dokumentId)
            )
        }

        val kodeStruktur = mapDialogmeldingKodeStruktur(record.dokumentasjonType)
        return DialogmeldingToBehandlerBestillingDTO(
            record.behandlerRef,
            record.personIdent,
            dialogmeldingUuid,
            null,
            UUID.randomUUID().toString(),
            kodeStruktur.dialogmeldingType,
            kodeStruktur.dialogmeldingKodeverk,
            kodeStruktur.dialogmeldingKode,
            null,
            pdfBrev,
            KILDE
        )
    }

    private fun mapDialogmeldingKodeStruktur(dokumentasjonType: DokumentasjonType): DialogmeldingKodeStruktur {
        return when (dokumentasjonType) {
            DokumentasjonType.L40 -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL, 1)
            DokumentasjonType.L8 -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL, 1)
            DokumentasjonType.MELDING_FRA_NAV -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_NOTAT, DialogmeldingKodeverk.HENVENDELSE, 8)
            DokumentasjonType.RETUR_LEGEERKLÆRING -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_NOTAT, DialogmeldingKodeverk.HENVENDELSE, 3)
            DokumentasjonType.PURRING -> DialogmeldingKodeStruktur(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL, 2)
            DokumentasjonType.L120 -> TODO() // TODO: Neste fase, lage brev og mapping for 120
        }
    }

    private data class DialogmeldingKodeStruktur(
        val dialogmeldingType: DialogmeldingType,
        val dialogmeldingKodeverk: DialogmeldingKodeverk,
        val dialogmeldingKode: Int
    )
}