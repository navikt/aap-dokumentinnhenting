package dokumentinnhenting.integrasjoner.syfo.bestilling

data class DialogmeldingToBehandlerBestillingDTO(
    val behandlerRef: String,
    val personIdent: String,
    val dialogmeldingUuid: String,
    val dialogmeldingRefParent: String?,
    val dialogmeldingRefConversation: String,
    val dialogmeldingType: String,
    val dialogmeldingKodeverk: String,
    val dialogmeldingKode: Int,
    val dialogmeldingTekst: String,
    val dialogmeldingVedlegg: ByteArray? = null,
)