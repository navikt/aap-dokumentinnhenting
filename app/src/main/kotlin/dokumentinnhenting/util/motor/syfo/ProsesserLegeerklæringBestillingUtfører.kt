package dokumentinnhenting.util.motor.syfo

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import java.util.*

class ProsesserLegeerklæringBestillingUtfører (
    private val prosesserStegService: ProsesserStegSyfoService,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val dialogmeldingUuId = UUID.fromString(input.parameter(BESTILLING_REFERANSE_PARAMETER_ID))
        prosesserStegService.prosesserBestilling(dialogmeldingUuId)
    }

    companion object : Jobb {
        const val BESTILLING_REFERANSE_PARAMETER_ID = "referanse"
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserLegeerklæringBestillingUtfører(
                ProsesserStegSyfoService.konstruer(
                    connection = connection
                )
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