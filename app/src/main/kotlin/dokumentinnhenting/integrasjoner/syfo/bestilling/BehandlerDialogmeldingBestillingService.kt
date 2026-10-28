package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.tilDialogmeldingRecord
import dokumentinnhenting.api.tilPåminnelseRecord
import dokumentinnhenting.prosessering.medDialogmeldingUuid
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.PåminnelseRepositoryImpl
import dokumentinnhenting.util.motor.syfo.ProsesserLegeerklæringBestillingUtfører
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

private val log = LoggerFactory.getLogger(BehandlerDialogmeldingBestillingService::class.java)

const val BESTILLING_REFERANSE_PARAMETER_NAVN = "referanse"
const val ANTALL_DAGER_PÅMINNELSE_FRIST = 22L

class BehandlerDialogmeldingBestillingService(
    private val connection: DBConnection,
) {
    private val jobbRepository = FlytJobbRepository(connection)
    private val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
    private val påminnelseRepository = PåminnelseRepositoryImpl(connection)

    companion object {
        fun konstruer(connection: DBConnection): BehandlerDialogmeldingBestillingService {
            return BehandlerDialogmeldingBestillingService(
                connection = connection
            )
        }
    }

    fun sendPåminnelseForBestilling(dialogmeldingUuid: UUID): UUID {
        val bestilling = dialogmeldingRepository.hentBestillingEldreEnn14Dager(dialogmeldingUuid)
            ?: throw RuntimeException("Fant ikke bestilling eldre enn 14 dager.")

        return bestillDialogmeldingPåminnelse(
            BehandlingsflytToDokumentInnhentingBestillingDto(
                bestillerNavIdent = bestilling.bestillerNavIdent,
                behandlerRef = bestilling.behandlerRef,
                behandlerNavn = bestilling.behandlerNavn,
                behandlerHprNr = bestilling.behandlerHprNr,
                personIdent = bestilling.personIdent,
                personNavn = bestilling.personNavn,
                dialogmeldingTekst = bestilling.fritekst,
                saksnummer = bestilling.saksnummer,
                dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.PURRING,
                behandlingsReferanse = bestilling.behandlingsReferanse,
                tidligereBestillingReferanse = bestilling.dialogmeldingUuid,
            )
        )
    }

    fun avbrytPåminnelseForBestilling(dialogmeldingUuid: UUID) {
        val bestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid)
            ?: throw IllegalArgumentException("Fant ikke bestilling med dialogmeldingUuid $dialogmeldingUuid")

        require(bestilling.dokumentasjonType == DokumentasjonType.L40 || bestilling.dokumentasjonType == DokumentasjonType.L8) {
            "Kan ikke avbryte påminnelse på en bestilling som ikke er forespørsel om L8 eller L40. Dialogmelding-UUID: $dialogmeldingUuid"
        }

        log.info("Avbryter automatisk påminnelse for bestilling med dialogmeldingUuid $dialogmeldingUuid")
        dialogmeldingRepository.settAutomatiskPåminnelse(
            automatiskPåminnelse = false,
            dialogmeldingUuid = dialogmeldingUuid
        )

        setPåminnelseStatus(dialogmeldingUuid, PåminnelseStatus.AVBRUTT)
    }


    fun gjenopptaPåminnelseForBestilling(dialogmeldingUuid: UUID) {
        val bestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid)
            ?: throw IllegalArgumentException("Fant ikke bestilling med dialogmeldingUuid $dialogmeldingUuid")

        require(bestilling.dokumentasjonType == DokumentasjonType.L40 || bestilling.dokumentasjonType == DokumentasjonType.L8) {
            "Kan ikke gjenoppta påminnelse på en bestilling som ikke er forespørsel om L8 eller L40. Dialogmelding-UUID: $dialogmeldingUuid"
        }
        log.info("Gjenopptar automatisk påminnelse for bestilling med dialogmeldingUuid $dialogmeldingUuid")
        dialogmeldingRepository.settAutomatiskPåminnelse(
            automatiskPåminnelse = true,
            dialogmeldingUuid = dialogmeldingUuid
        )

        setPåminnelseStatus(dialogmeldingUuid, PåminnelseStatus.PLANLAGT)
    }

    fun sendAutomatiskPåminnelseHvisBestillingFinnes(
        behandlingReferanse: BehandlingReferanse,
        bestillingOpprettetDato: LocalDate
    ) {
        val bestillingerSomSkalPåminnes = finnBestillingerSomSkalPåminnes(behandlingReferanse, bestillingOpprettetDato)
        bestillingerSomSkalPåminnes.forEach {
            log.info("Sender purring på behandling $behandlingReferanse på sak ${it.saksnummer} for opprinnelig bestilling med id ${it.dialogmeldingUuid}")
            bestillDialogmeldingPåminnelse(
                BehandlingsflytToDokumentInnhentingBestillingDto(
                    bestillerNavIdent = it.bestillerNavIdent,
                    behandlerRef = it.behandlerRef,
                    behandlerNavn = it.behandlerNavn,
                    behandlerHprNr = it.behandlerHprNr,
                    personIdent = it.personIdent,
                    personNavn = it.personNavn,
                    dialogmeldingTekst = it.fritekst,
                    saksnummer = it.saksnummer,
                    dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.PURRING,
                    behandlingsReferanse = it.behandlingsReferanse,
                    tidligereBestillingReferanse = it.dialogmeldingUuid,
                )
            )
            connection.markerSavepoint()
        }
    }

    private fun finnBestillingerSomSkalPåminnes(
        behandlingsreferanse: BehandlingReferanse,
        bestillingOpprettetDato: LocalDate
    ): List<DialogmeldingFullRecord> {
        val bestillinger = dialogmeldingRepository.hentBestillingerSomSkalPåminnes(
            behandlingReferanse = behandlingsreferanse,
            dokumentasjonstype = DokumentasjonType.L40,
            opprettetDato = bestillingOpprettetDato
        )
        val påminnelser = påminnelseRepository.hentPåminnelseForBehandlingsReferanse(behandlingsreferanse)
        val bestillingerMedPåminnelse = bestillinger.filter{ skalBestillingPåminnes(it, påminnelser) }

        log.info("Fant ${bestillingerMedPåminnelse.size} bestillinger som skal purres på for behandling $behandlingsreferanse")

        return bestillingerMedPåminnelse
    }

    private fun skalBestillingPåminnes(bestilling: DialogmeldingFullRecord, påminnelser: List<PåminnelseRecord>): Boolean {
        val sisteRelevantePåminnelse = påminnelser
            .filter { it.dialogmeldingId.equals(bestilling.dialogmeldingUuid) }
            .maxByOrNull { it.opprettetTidspunkt }

        // Er null for forespørslene som ble sendt før opprettelse av Paaminnelsetabellen
        return sisteRelevantePåminnelse == null || sisteRelevantePåminnelse.status == PåminnelseStatus.PLANLAGT
    }

    fun bestillDialogmeldingPåminnelse(dto: BehandlingsflytToDokumentInnhentingBestillingDto, samtaleRef: UUID? = null): UUID {
        val dialogmeldingUuid = UUID.randomUUID()
        val dialogMeldingRecord = dto.tilDialogmeldingRecord(dialogmeldingUuid, samtaleRef)

        return dialogmeldingBestilling(dialogMeldingRecord)
    }

    // TODO: Forslag til et mer passende navn her? For å skille mellom ny forespørsel og påminnelse
    fun bestillNyDialogmelding(dto: BehandlingsflytToDokumentInnhentingBestillingDto, samtaleRef: UUID? = null): UUID {
        val dialogmeldingUuid = UUID.randomUUID()
        val dialogMeldingRecord = dto.tilDialogmeldingRecord(dialogmeldingUuid, samtaleRef)

        opprettPåminnelseHvisNødvendig(dialogMeldingRecord)

        return dialogmeldingBestilling(dialogMeldingRecord)
    }

    fun dialogmeldingBestilling(dialogmeldingRecord: DialogmeldingRecord): UUID {
        val id = skrivDialogmeldingTilRepository(dialogmeldingRecord)
        val jobb =
            JobbInput(ProsesserLegeerklæringBestillingUtfører)
                .medCallId()
                .medParameter(BESTILLING_REFERANSE_PARAMETER_NAVN, id.toString())
                .medDialogmeldingUuid(dialogmeldingRecord.dialogmeldingUuid)

        jobbRepository.leggTil(jobb)

        return id
    }

    private fun skrivDialogmeldingTilRepository(melding: DialogmeldingRecord): UUID {
        log.info("Mottatt dialogmelding-bestilling på sak ${melding.saksnummer}")
        return dialogmeldingRepository.opprettDialogmelding(melding)
    }

    private fun opprettPåminnelseHvisNødvendig(dialogmelding: DialogmeldingRecord) {
        log.info("Oppretter påminnelseobjekt i repository for sak ${dialogmelding.saksnummer})")
        påminnelseRepository.opprettPåminnelseVedBehov(
            dialogmelding.tilPåminnelseRecord(ANTALL_DAGER_PÅMINNELSE_FRIST)
        )
    }

    private fun setPåminnelseStatus(dialogmeldingId: UUID, status: PåminnelseStatus) {
        log.info("Setter påminnelsestatus ${status} for dialogmeldingId ${dialogmeldingId}")
        påminnelseRepository.oppdaterPåminnelseStatus(dialogmeldingId, PåminnelseStatus.AVBRUTT)
    }
}
