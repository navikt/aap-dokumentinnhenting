package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.util.*

data class BehandlingsflytToDokumentInnhentingBestillingDTO(
    val bestillerNavIdent: String,
    val behandlerRef: String,
    val behandlerNavn: String,
    val behandlerHprNr: String,
    val personIdent: String,
    val personNavn: String,
    val dialogmeldingTekst: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val behandlingsReferanse: UUID,
    val tidligereBestillingReferanse: UUID? = null
)

enum class DokumentasjonType {
    L40, L8, L120, MELDING_FRA_NAV, RETUR_LEGEERKLÆRING, PURRING
}