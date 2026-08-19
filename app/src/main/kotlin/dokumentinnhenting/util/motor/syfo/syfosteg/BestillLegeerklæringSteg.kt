package dokumentinnhenting.util.motor.syfo.syfosteg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.integrasjoner.saf.SafRestGateway
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingBrevGeneratorService
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingKode
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingKodeverk
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingToBehandlerBestillingDTO
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingType
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.kafkaProducer
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.metrics.bestillingCounter
import dokumentinnhenting.util.metrics.prometheus
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

const val SYFO_BESTILLING_DIALOGMELDING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
const val KILDE = "AAP"

class BestillLegeerklæringSteg(
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val brevGeneratorService: DialogmeldingBrevGeneratorService,
    private val producer: KafkaProducer<String, String>,
) : SyfoSteg.Utfører {
    private val objectMapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(StartLegeerklæringBestillingSteg::class.java)

    override fun utfør(kontekst: SyfoSteg.Kontekst): SyfoSteg.Resultat {
        log.info("BestillLegeerklæringSteg")
        return sendBestilling(kontekst.referanse)
    }

    companion object : SyfoSteg {
        override fun konstruer(connection: DBConnection): SyfoSteg.Utfører {
            return BestillLegeerklæringSteg(
                dialogmeldingRepository = DialogmeldingRepository(connection),
                brevGeneratorService = DialogmeldingBrevGeneratorService(BrevGateway()),
                producer = kafkaProducer
            )
        }
    }

    private fun sendBestilling(dialogmeldingUuid: UUID): SyfoSteg.Resultat {
        val funnetBestilling = requireNotNull(dialogmeldingRepository.hentByDialogId(dialogmeldingUuid)) {
            "Fant ingen bestilling for dialogmelding $dialogmeldingUuid"
        }
        val tidligereTilhørendeBestillingsdato = funnetBestilling.tidligereBestillingReferanse?.let {
            dialogmeldingRepository.hentByDialogId(it)?.opprettet
        }
        val mappedBestilling = mapToDialogMeldingBestilling(
            dialogmeldingUuid = dialogmeldingUuid,
            record = funnetBestilling,
            tidligereTilhørendeBestillingsdato = tidligereTilhørendeBestillingsdato
        )

        val jsonValue = objectMapper.writeValueAsString(mappedBestilling)

        val record = ProducerRecord(
            SYFO_BESTILLING_DIALOGMELDING_TOPIC,
            mappedBestilling.dialogmeldingUuid.toString(),
            jsonValue
        )

        try {
            producer.send(record).get()
        } catch (e: Exception) {
            log.error("Feilet ved sending til topic $SYFO_BESTILLING_DIALOGMELDING_TOPIC", e)
            return SyfoSteg.Resultat.STOPP
        }
        prometheus.bestillingCounter(SYFO_BESTILLING_DIALOGMELDING_TOPIC).increment()

        return SyfoSteg.Resultat.FULLFØRT
    }

    private fun mapToDialogMeldingBestilling(
        dialogmeldingUuid: UUID,
        record: DialogmeldingFullRecord,
        tidligereTilhørendeBestillingsdato: LocalDateTime?
    ): DialogmeldingToBehandlerBestillingDTO {
        val pdfBrev: ByteArray = runBlocking {
            SafRestGateway.hentDokumentMedJournalpostId(
                requireNotNull(record.journalpostId), requireNotNull(record.dokumentId)
            )
        }

        val kodeStruktur = mapDialogmeldingKodeStruktur(record.dokumentasjonType)
        val dialogmelding = runBlocking {
            brevGeneratorService.genererMedSignatur(
                personNavn = record.personNavn,
                personIdent = record.personIdent,
                dialogmeldingTekst = record.fritekst,
                dokumentasjonType = record.dokumentasjonType,
                tidligereBestillingDato = tidligereTilhørendeBestillingsdato,
                bestillerNavIdent = record.bestillerNavIdent,
            )
        }
        return DialogmeldingToBehandlerBestillingDTO(
            behandlerRef = record.behandlerRef,
            personIdent = record.personIdent,
            dialogmeldingUuid = dialogmeldingUuid,
            dialogmeldingRefParent = null,
            dialogmeldingRefConversation = record.samtaleRef.toString(),
            dialogmeldingType = kodeStruktur.dialogmeldingType,
            dialogmeldingKodeverk = kodeStruktur.dialogmeldingKodeverk,
            dialogmeldingKode = kodeStruktur.dialogmeldingKode,
            dialogmeldingTekst = dialogmelding,
            dialogmeldingVedlegg = pdfBrev,
            kilde = KILDE
        )
    }

    private fun mapDialogmeldingKodeStruktur(dokumentasjonType: DokumentasjonType): DialogmeldingKodeStruktur {
        return when (dokumentasjonType) {
            DokumentasjonType.L40 -> DialogmeldingKodeStruktur(
                DialogmeldingType.DIALOG_FORESPORSEL,
                DialogmeldingKodeverk.FORESPORSEL,
                DialogmeldingKode.FORESPØRSEL_OM_PASIENT
            )

            DokumentasjonType.L8 -> DialogmeldingKodeStruktur(
                DialogmeldingType.DIALOG_FORESPORSEL,
                DialogmeldingKodeverk.FORESPORSEL,
                DialogmeldingKode.FORESPØRSEL_OM_PASIENT
            )

            DokumentasjonType.MELDING_FRA_NAV -> DialogmeldingKodeStruktur(
                DialogmeldingType.DIALOG_NOTAT,
                DialogmeldingKodeverk.HENVENDELSE,
                DialogmeldingKode.MELDING_FRA_NAV
            )

            DokumentasjonType.RETUR_LEGEERKLÆRING -> DialogmeldingKodeStruktur(
                DialogmeldingType.DIALOG_NOTAT,
                DialogmeldingKodeverk.HENVENDELSE,
                DialogmeldingKode.RETUR_AV_LEGEERKLÆRING
            )

            DokumentasjonType.PURRING -> DialogmeldingKodeStruktur(
                DialogmeldingType.DIALOG_FORESPORSEL,
                DialogmeldingKodeverk.FORESPORSEL,
                DialogmeldingKode.PÅMINNELSE_FORESPORSEL_OM_PASIENT
            )

            DokumentasjonType.L120 -> TODO() // TODO: Neste fase, lage brev og mapping for 120
        }
    }

    private data class DialogmeldingKodeStruktur(
        val dialogmeldingType: DialogmeldingType,
        val dialogmeldingKodeverk: DialogmeldingKodeverk,
        val dialogmeldingKode: DialogmeldingKode
    )
}
