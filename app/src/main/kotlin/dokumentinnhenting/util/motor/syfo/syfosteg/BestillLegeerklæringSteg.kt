package dokumentinnhenting.util.motor.syfo.syfosteg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dokumentinnhenting.integrasjoner.syfo.bestilling.*
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.kafka.config.ProducerConfig
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

const val SYFO_BESTILLING_DIALOGMELDING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"

class BestillLegeerklæringSteg(
   // monitor: Events, //TODO: Få denne inn
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val producer: KafkaProducer<String, String> = KafkaProducer(ProducerConfig().properties()),

): SyfoSteg.Utfører {
    private val objectMapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(StartLegeerklæringBestillingSteg::class.java)
    //TODO: Få denne inn
    /*
    init {
        monitor.subscribe(ApplicationStopped) {
            producer.close()
        }
    }*/

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
        val funnetBestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid)
        val mappedBestilling = mapToDialogMeldingBestilling(dialogmeldingUuid, funnetBestilling)

        val jsonValue = objectMapper.writeValueAsString(mappedBestilling)

        val record = ProducerRecord(SYFO_BESTILLING_DIALOGMELDING_TOPIC, mappedBestilling.dialogmeldingUuid, jsonValue)
        try {
            producer.send(record).get()
        } catch (e: Exception) {
            log.error("Feilet ved sending til topic $SYFO_BESTILLING_DIALOGMELDING_TOPIC", e)
            return SyfoSteg.Resultat.STOPP
        }

        return SyfoSteg.Resultat.FULLFØRT
    }

    private fun mapToDialogMeldingBestilling(dialogmeldingUuid: UUID, dto: BehandlingsflytToDialogmeldingDTO): DialogmeldingToBehandlerBestillingDTO {
        val brevTekst = genererBrev(dto)
        val kodeStruktur = mapDialogmeldingKodeStruktur(dto.dokumentasjonType)
        return DialogmeldingToBehandlerBestillingDTO(
            dto.behandlerRef,
            dto.personIdent,
            dialogmeldingUuid.toString(),
            null, // Trenger vi denne?
            UUID.randomUUID().toString(), // Trenger vi denne?
            kodeStruktur.dialogmeldingType,
            kodeStruktur.dialogmeldingKodeverk,
            kodeStruktur.dialogmeldingKode,
            brevTekst,
            null
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

    private data class DialogmeldingKodeStruktur(
        val dialogmeldingType: DialogmeldingType,
        val dialogmeldingKodeverk: DialogmeldingKodeverk,
        val dialogmeldingKode: Int
    )
}