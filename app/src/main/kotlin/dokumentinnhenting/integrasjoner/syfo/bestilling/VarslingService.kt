package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.unleash.FeatureToggles
import dokumentinnhenting.unleash.UnleashGateway

class VarslingService(
    private val unleash: UnleashGateway
) {
    fun skalVarsleBruker(dokumentasjonType: DokumentasjonType): Boolean = when (dokumentasjonType) {
        DokumentasjonType.L40, DokumentasjonType.L8, DokumentasjonType.L120 -> true
        DokumentasjonType.PURRING -> unleash.isEnabled(FeatureToggles.VarsleBrukerVedPurring)
        DokumentasjonType.MELDING_FRA_NAV, DokumentasjonType.RETUR_LEGEERKLÆRING -> false
    }
}
