package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.fraDto
import dokumentinnhenting.prosessering.medDialogmeldingUuid
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.ProsesserLegeerklæringBestillingUtfører
import java.util.UUID
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BehandlerDialogmeldingBestillingService::class.java)

class BehandlerDialogmeldingBestillingService(
    private val jobbRepository: FlytJobbRepository,
    private val dialogmeldingRepository: DialogmeldingRepository,
) {
    companion object {
        fun konstruer(connection: DBConnection): BehandlerDialogmeldingBestillingService {
            return BehandlerDialogmeldingBestillingService(
                jobbRepository = FlytJobbRepository(connection),
                dialogmeldingRepository = DialogmeldingRepository(connection)
            )
        }
    }

    fun dialogmeldingPurring(dto: LegeerklæringPurringDto): UUID {
        val bestilling = dialogmeldingRepository.hentBestillingEldreEnn14Dager(requireNotNull(dto.dialogmeldingUuid))
            ?: throw RuntimeException("Fant ikke bestilling eldre enn 14 dager.")

        return dialogmeldingBestilling(
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

    fun dialogmeldingBestilling(dto: BehandlingsflytToDokumentInnhentingBestillingDto, samtaleRef: UUID? = null): UUID {
        val dialogmeldingUuid = UUID.randomUUID()
        val dialogMeldingRecord = DialogmeldingRecord(
            bestillerNavIdent = dto.bestillerNavIdent,
            dialogmeldingUuid = dialogmeldingUuid,
            behandlerRef = dto.behandlerRef,
            behandlerHprNr = dto.behandlerHprNr,
            personIdent = dto.personIdent,
            personNavn = dto.personNavn,
            saksnummer = dto.saksnummer,
            dokumentasjonType = dto.dokumentasjonType.fraDto(),
            behandlerNavn = dto.behandlerNavn,
            fritekst = dto.dialogmeldingTekst,
            behandlingsReferanse = dto.behandlingsReferanse,
            tidligereBestillingReferanse = dto.tidligereBestillingReferanse,
            samtaleRef = samtaleRef ?: dialogmeldingUuid,
        )

        val id = skrivDialogmeldingTilRepository(dialogMeldingRecord)
        val BESTILLING_REFERANSE_PARAMETER_NAVN = "referanse"

        val jobb =
            JobbInput(ProsesserLegeerklæringBestillingUtfører)
                .medCallId()
                .medParameter(BESTILLING_REFERANSE_PARAMETER_NAVN, id.toString())
                .medDialogmeldingUuid(dialogMeldingRecord.dialogmeldingUuid)

        jobbRepository.leggTil(jobb)

        return id
    }

    private fun skrivDialogmeldingTilRepository(melding: DialogmeldingRecord): UUID {
        log.info("Mottatt dialogmelding-bestilling på sak ${melding.saksnummer}")
        return dialogmeldingRepository.opprettDialogmelding(melding)
    }
}
