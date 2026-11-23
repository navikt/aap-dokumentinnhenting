package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.tilDialogmeldingRecord
import dokumentinnhenting.api.tilPåminnelseRecord
import dokumentinnhenting.prosessering.medDialogmeldingUuid
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.PåminnelseHistorikkRepositoryImpl
import dokumentinnhenting.repositories.PåminnelseRepositoryImpl
import dokumentinnhenting.unleash.FeatureToggles
import dokumentinnhenting.unleash.UnleashGateway
import dokumentinnhenting.util.DialogmeldingUUID
import dokumentinnhenting.util.motor.syfo.ProsesserLegeerklæringBestillingUtfører
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val log = LoggerFactory.getLogger(BehandlerDialogmeldingBestillingService::class.java)

const val BESTILLING_REFERANSE_PARAMETER_NAVN = "referanse"

class BehandlerDialogmeldingBestillingService(
    private val connection: DBConnection,
    private val unleash: UnleashGateway,
) {
    private val jobbRepository = FlytJobbRepository(connection)
    private val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
    private val påminnelseRepository = PåminnelseRepositoryImpl(connection)
    private val påminnelseHistorikkRepository = PåminnelseHistorikkRepositoryImpl(connection)

    companion object {
        fun konstruer(connection: DBConnection, unleash: UnleashGateway): BehandlerDialogmeldingBestillingService {
            return BehandlerDialogmeldingBestillingService(
                connection = connection,
                unleash = unleash,
            )
        }
    }

    fun sendPåminnelseForBestilling(dialogmeldingUuid: DialogmeldingUUID, bestillerNavIdent: Bruker?): UUID {
        val bestilling = dialogmeldingRepository.hentBestillingEldreEnnAntallDager(
            dialogmeldingUuid.uuid,
            DIALOGMELDING_PÅMINNELSE_HENT_BESTILLINGER_ELDRE_ENN
        ) ?: throw RuntimeException("Fant ikke bestilling eldre enn 1 dag hvis bruk til påminnelse i test, eller 14 dager hvis prod eller til bruk i brevpreview.")

        if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
            opprettEllerOppdaterPåminnelseEksisterendeForespørsel(
                bestilling,
                PåminnelseStatus.PLANLAGT,
                bestillerNavIdent
            )
        }

        return bestillDialogmeldingPåminnelse(
            dto = BehandlingsflytToDokumentInnhentingBestillingDto(
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
            ),
            samtaleRef = bestilling.samtaleRef
        )
    }

    fun validerBehandlingsreferanseForBestilling(dialogmeldingUuid: UUID, behandlingsreferanse: UUID) {
        val bestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid)
            ?: throw IllegalArgumentException("Fant ingen bestilling for dialogmeldingUuid $dialogmeldingUuid")
        if (behandlingsreferanse != bestilling.behandlingsReferanse) {
            throw IkkeTillattException("Behandlingsreferanse i request stemmer ikke overens med behandlingsreferanse i bestilling.")
        }
    }

    fun avbrytPåminnelseForBestilling(dialogmeldingUuid: DialogmeldingUUID, bestillerNavIdent: Bruker?) {
        val bestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid.uuid)
            ?: throw IllegalArgumentException("Fant ikke bestilling med dialogmeldingUuid $dialogmeldingUuid")

        require(bestilling.dokumentasjonType == DokumentasjonType.L40 || bestilling.dokumentasjonType == DokumentasjonType.L8) {
            "Kan ikke avbryte påminnelse på en bestilling som ikke er forespørsel om L8 eller L40. Dialogmelding-UUID: $dialogmeldingUuid"
        }

        log.info("Avbryter automatisk påminnelse for bestilling med dialogmeldingUuid $dialogmeldingUuid")
        dialogmeldingRepository.settAutomatiskPåminnelse(
            automatiskPåminnelse = false,
            dialogmeldingUuid = dialogmeldingUuid.uuid
        )

        if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
            opprettEllerOppdaterPåminnelseEksisterendeForespørsel(
                bestilling,
                PåminnelseStatus.AVBRUTT,
                bestillerNavIdent
            )
        }
    }


    fun gjenopptaPåminnelseForBestilling(dialogmeldingUuid: DialogmeldingUUID, bestillerNavIdent: Bruker?) {
        val bestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid.uuid)
            ?: throw IllegalArgumentException("Fant ikke bestilling med dialogmeldingUuid $dialogmeldingUuid")

        require(bestilling.dokumentasjonType == DokumentasjonType.L40 || bestilling.dokumentasjonType == DokumentasjonType.L8) {
            "Kan ikke gjenoppta påminnelse på en bestilling som ikke er forespørsel om L8 eller L40. Dialogmelding-UUID: $dialogmeldingUuid"
        }
        log.info("Gjenopptar automatisk påminnelse for bestilling med dialogmeldingUuid $dialogmeldingUuid")
        dialogmeldingRepository.settAutomatiskPåminnelse(
            automatiskPåminnelse = true,
            dialogmeldingUuid = dialogmeldingUuid.uuid
        )

        if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
            opprettEllerOppdaterPåminnelseEksisterendeForespørsel(
                bestilling,
                PåminnelseStatus.PLANLAGT,
                bestillerNavIdent
            )
        }
    }

    fun sendAutomatiskPåminnelseHvisBestillingFinnes(
        behandlingReferanse: BehandlingReferanse,
        bestillingOpprettetDato: LocalDate
    ) {
        val bestillingerSomSkalPåminnes = finnBestillingerSomSkalPåminnes(behandlingReferanse, bestillingOpprettetDato)
        bestillingerSomSkalPåminnes.forEach {
            log.info("Sender purring på behandling $behandlingReferanse på sak ${it.saksnummer} for opprinnelig bestilling med id ${it.dialogmeldingUuid}")
            bestillDialogmeldingPåminnelse(
                dto = BehandlingsflytToDokumentInnhentingBestillingDto(
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
                ),
                samtaleRef = it.samtaleRef
            )
            if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
                påminnelseRepository.opprettPåminnelseEllerOppdaterStatus(
                    PåminnelseRecord(
                        id = null,
                        dialogmeldingId = it.dialogmeldingUuid,
                        behandlingsReferanse = behandlingReferanse.referanse,
                        status = PåminnelseStatus.PLANLAGT,
                        påminnelseDato = bestillingOpprettetDato.plusDays(PÅMINNELSE_ANTALL_DAGER_TREKKE_FRA),
                        opprettetTidspunkt = it.opprettet,
                        sisteForespørselSendt = null
                    )
                )
            }
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

        if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
            val påminnelser = påminnelseRepository.hentPåminnelseForBehandlingsReferanse(behandlingsreferanse)
            val bestillingerMedPåminnelse = bestillinger.filter { skalBestillingPåminnes(it, påminnelser) }

            log.info("Fant ${bestillingerMedPåminnelse.size} bestillinger som skal purres på for behandling $behandlingsreferanse")

            return bestillingerMedPåminnelse
        }

        log.info("Fant ${bestillinger.size} bestillinger som skal purres på for behandling $behandlingsreferanse")

        return bestillinger
    }

    private fun skalBestillingPåminnes(
        bestilling: DialogmeldingFullRecord,
        påminnelser: List<PåminnelseRecord>
    ): Boolean {
        val sisteRelevantePåminnelse = påminnelser
            .filter { it.dialogmeldingId == bestilling.dialogmeldingUuid }
            .maxByOrNull { it.opprettetTidspunkt }

        // Er null for forespørslene som ble sendt før opprettelse av påminnelsetabellen
        return sisteRelevantePåminnelse == null || sisteRelevantePåminnelse.status == PåminnelseStatus.PLANLAGT
    }

    fun bestillDialogmeldingPåminnelse(dto: BehandlingsflytToDokumentInnhentingBestillingDto, samtaleRef: UUID): UUID {
        val dialogmeldingUuid = UUID.randomUUID()
        val dialogMeldingRecord = dto.tilDialogmeldingRecord(dialogmeldingUuid, samtaleRef)

        val dialogmeldingUUID = lagreDialogmeldingBestillingOgOpprettJobb(dialogMeldingRecord)

        return dialogmeldingUUID
    }

    fun bestillNyDialogmeldingForespørsel(dto: BehandlingsflytToDokumentInnhentingBestillingDto): UUID {
        val dialogmeldingUuid = UUID.randomUUID()
        val dialogMeldingRecord = dto.tilDialogmeldingRecord(dialogmeldingUuid = dialogmeldingUuid, samtaleRef = null)

        val dialogmeldingUUID = lagreDialogmeldingBestillingOgOpprettJobb(dialogMeldingRecord)

        if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
            opprettEllerOppdaterPåminnelseRecord(
                dialogMeldingRecord,
                Bruker(dto.bestillerNavIdent)
            )
        }

        return dialogmeldingUUID
    }

    fun lagreDialogmeldingBestillingOgOpprettJobb(dialogmeldingRecord: DialogmeldingRecord): UUID {
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

    private fun opprettEllerOppdaterPåminnelseRecord(dialogmelding: DialogmeldingRecord, bestillerNavIdent: Bruker?) {
        log.info("Oppretter påminnelseobjekt i repository for sak ${dialogmelding.saksnummer}")

        val nyPåminnelseRecord = dialogmelding.tilPåminnelseRecord(
            påminnelseDato = LocalDate.now().plusDays(PÅMINNELSE_ANTALL_DAGER_TREKKE_FRA),
            opprettetTidspunkt = LocalDateTime.now()
        )
        val påminnelseId = påminnelseRepository.opprettPåminnelseEllerOppdaterStatus(nyPåminnelseRecord)
        val påminnelseRecordMedId = nyPåminnelseRecord.copy(id = påminnelseId)

        if (unleash.isEnabled(FeatureToggles.LagrePaaminnelseHistorikk) && bestillerNavIdent != null) {
            opprettPåminnelseStatusEndringHistorikk(påminnelseRecordMedId, bestillerNavIdent)
        }
    }

    private fun opprettEllerOppdaterPåminnelseEksisterendeForespørsel(
        dialogmelding: DialogmeldingFullRecord,
        status: PåminnelseStatus,
        bestillerNavIdent: Bruker?
    ) {
        log.info("Oppretter påminnelseobjekt i repository for sak ${dialogmelding.saksnummer}")

        val påminnelseId = påminnelseRepository.opprettPåminnelseEllerOppdaterStatus(
            dialogmelding.tilPåminnelseRecord(
                påminnelseStatus = status
            )
        )

        val påminnelseMedId = påminnelseRepository
            .hentPåminnelseForDialogmeldingId(dialogmelding.dialogmeldingUuid)
            .single { it.id == påminnelseId }

        if (unleash.isEnabled(FeatureToggles.LagrePaaminnelseHistorikk) && bestillerNavIdent != null) {
            opprettPåminnelseStatusEndringHistorikk(
                påminnelseMedId,
                bestillerNavIdent
            )
        }
    }

    private fun opprettPåminnelseStatusEndringHistorikk(påminnelse: PåminnelseRecord, bestillerNavIdent: Bruker) {
        log.info("Oppretter historikkobjekt i repository med status ${påminnelse.status} for dialogmelding med id ${påminnelse.dialogmeldingId}")
        påminnelseHistorikkRepository.lagreNyPåminnelseStatusEndring(
            påminnelse,
            bestillerNavIdent,
            LocalDateTime.now()
        )
    }
}
