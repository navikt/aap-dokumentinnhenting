package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import java.util.*

class ProsesserLegeerklæringBestillingUtfører (
    private val prosesserStegService: ProsesserStegSyfoService,
    private val dialogmeldingRepository: DialogmeldingRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val dialogmeldingUuId = UUID.fromString(input.parameter(BESTILLING_REFERANSE_PARAMETER_ID))
        val bestillingId = dialogmeldingRepository.låsBestilling(dialogmeldingUuId)
        prosesserStegService.prosesserBestilling(bestillingId)
    }

    companion object : Jobb {
        const val BESTILLING_REFERANSE_PARAMETER_ID = "referanse"
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserLegeerklæringBestillingUtfører(
                ProsesserStegSyfoService.konstruer(
                    connection = connection
                ),
                DialogmeldingRepository(connection)
            )
        }

        override fun type(): String {
            return "prosesserLegeerklæringBestilling"
        }

        override fun navn(): String {
            return "Prosesser legeerklæring bestilling"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å gjennomføre bestilling av legeerklæringer"
        }
    }
}