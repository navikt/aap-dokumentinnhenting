package dokumentinnhenting.integrasjoner.syfo.bestilling

data class BehandlingsflytToDialogmeldingDTO(
    val behandlerRef: String,               // Referanse til en bestemt behandler, definert i vårt system
    val personIdent: String,                // Personident til den sykmeldte
    val dialogmeldingTekst: String,          //or Null Innholdet i dialogmeldingen. Kan være en stringserialisering av et dokument på et strukturert format.
    val dialogmeldingVedlegg: ByteArray?,    //Vedlegg til dialogmeldingen, en PDF på byte-array format.
    val sakId: String,
    val dokumentasjonType: DokumentasjonType
)

enum class DokumentasjonType {
    L40, L8, L120, MELDING_FRA_NAV, RETUR_LEGEERKLÆRING
}