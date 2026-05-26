package dokumentinnhenting.integrasjoner.syfo.bestilling

enum class DokumentasjonType {
    L40, L8, L120, MELDING_FRA_NAV, RETUR_LEGEERKLÆRING, PURRING;

    fun skalVarsleBruker(): Boolean = when (this) {
        L40, L8, L120 -> true
        else -> false
    }
}
