package dokumentinnhenting.util.motor

import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører
import dokumentinnhenting.util.motor.syfo.ProsesserLegeerklæringBestillingUtfører
import no.nav.aap.motor.Jobb

/**
* Alle oppgavene som skal utføres i systemet
*/
object ProsesseringsJobber {
    fun alle(): List<Jobb> {
        return listOf(
            ProsesserLegeerklæringBestillingUtfører,
            OppdaterLegeerklæringStatusUtfører
        )
    }
}