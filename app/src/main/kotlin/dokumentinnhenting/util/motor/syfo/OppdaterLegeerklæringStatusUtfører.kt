package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import java.util.*

class OppdaterLegeerklæringStatusUtfører (
    private val dialogmeldingRepository: DialogmeldingRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val record = DefaultJsonMapper.fromJson<DialogmeldingStatusDTO>(input.payload())
        val bestillingId = dialogmeldingRepository.låsBestilling(UUID.fromString(record.bestillingUuid))

        dialogmeldingRepository.oppdaterDialogmeldingStatus(record)

        if (record.status == MeldingStatusType.AVVIST) {
            val sak = dialogmeldingRepository.hentByDialogId(bestillingId)
            val behandlingsflytClient = BehandlingsflytClient()
            behandlingsflytClient.taSakAvVent(sak.behandlingsReferanse, BehandlingsflytClient.TaAvVentRequest(0L, "Avvist hos SYFO med begrunnelse: ${record.tekst}"))
        }
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OppdaterLegeerklæringStatusUtfører(
                DialogmeldingRepository(connection)
            )
        }

        override fun type(): String {
            return "oppdaterStatusLegeerklæring"
        }

        override fun navn(): String {
            return "Oppdaterer status på bestilling av legeerklæring"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å gjennomføre statusoppdatering på legeerklæringer"
        }
    }
}