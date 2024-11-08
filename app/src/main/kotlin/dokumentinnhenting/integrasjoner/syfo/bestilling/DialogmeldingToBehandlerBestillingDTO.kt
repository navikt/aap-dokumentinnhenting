package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.util.*

data class DialogmeldingToBehandlerBestillingDTO(
    val behandlerRef: String,
    val personIdent: String,
    val dialogmeldingUuid: UUID,
    val dialogmeldingRefParent: String?,
    val dialogmeldingRefConversation: String,
    val dialogmeldingType: DialogmeldingType,
    val dialogmeldingKodeverk: DialogmeldingKodeverk,
    val dialogmeldingKode: Int,
    val dialogmeldingTekst: String,
    val dialogmeldingVedlegg: ByteArray? = null,
)

enum class DialogmeldingType {
    DIALOG_FORESPORSEL, DIALOG_NOTAT
}

enum class DialogmeldingKodeverk {
    HENVENDELSE, FORESPORSEL
}