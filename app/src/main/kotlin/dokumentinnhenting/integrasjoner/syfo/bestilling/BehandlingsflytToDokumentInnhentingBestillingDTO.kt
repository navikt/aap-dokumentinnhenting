package dokumentinnhenting.integrasjoner.syfo.bestilling

data class BehandlingsflytToDokumentInnhentingBestillingDTO(
    val behandlerRef: String,
    val behandlerNavn: String,
    val veilederNavn: String,
    val personIdent: String,
    val personNavn: String,
    val dialogmeldingTekst: String,
    val dialogmeldingVedlegg: ByteArray?,    //Vedlegg til dialogmeldingen, en PDF på byte-array format.
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType
)

enum class DokumentasjonType {
    L40, L8, L120, MELDING_FRA_NAV, RETUR_LEGEERKLÆRING
}