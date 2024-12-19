package dokumentinnhenting.util.motor

import dokumentinnhenting.integrasjoner.behandlingsflyt.jobber.TaSakAvVentUtfører
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.EndreTemaUtfører
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.HåndterMottattDialogmeldingUtfører
import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører
import dokumentinnhenting.util.motor.syfo.ProsesserLegeerklæringBestillingUtfører
import dokumentinnhenting.util.motor.syfo.SendVarslingsbrevUtfører
import no.nav.aap.motor.Jobb

/**
* Alle oppgavene som skal utføres i systemet
*/
object ProsesseringsJobber {
    fun alle(): List<Jobb> {
        return listOf(
            ProsesserLegeerklæringBestillingUtfører,
            OppdaterLegeerklæringStatusUtfører,
            SendVarslingsbrevUtfører,
            HåndterMottattDialogmeldingUtfører,
            TaSakAvVentUtfører,
            EndreTemaUtfører
        )
    }
}
