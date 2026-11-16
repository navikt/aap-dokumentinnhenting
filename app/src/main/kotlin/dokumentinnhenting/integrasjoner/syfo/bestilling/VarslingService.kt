package dokumentinnhenting.integrasjoner.syfo.bestilling

class VarslingService {
    fun skalVarsleBruker(dokumentasjonType: DokumentasjonType): Boolean = when (dokumentasjonType) {
        DokumentasjonType.L40,
        DokumentasjonType.L8,
        DokumentasjonType.L120,
        DokumentasjonType.PURRING -> true

        DokumentasjonType.MELDING_FRA_NAV,
        DokumentasjonType.RETUR_LEGEERKLÆRING -> false
    }
}
