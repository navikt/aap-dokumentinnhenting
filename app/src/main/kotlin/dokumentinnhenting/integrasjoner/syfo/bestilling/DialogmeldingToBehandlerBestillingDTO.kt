package dokumentinnhenting.integrasjoner.syfo.bestilling

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class DialogmeldingToBehandlerBestillingDTO(
    val behandlerRef: String,
    val personIdent: String,
    val dialogmeldingUuid: UUID,
    val dialogmeldingRefParent: String?,
    val dialogmeldingRefConversation: String,
    val dialogmeldingType: DialogmeldingType,
    val dialogmeldingKodeverk: DialogmeldingKodeverk,
    val dialogmeldingKode: DialogmeldingKode,
    val dialogmeldingTekst: String?,
    val dialogmeldingVedlegg: ByteArray,
    val kilde: String,
)

enum class DialogmeldingType {
    DIALOG_FORESPORSEL, DIALOG_NOTAT
}

enum class DialogmeldingKodeverk {
    HENVENDELSE, FORESPORSEL
}

/*
* Verdier hentet herfra:
* https://github.com/navikt/isdialogmelding/blob/master/documentation/kafka/isdialogmelding-behandler-dialogmelding-bestilling.md
*/
enum class DialogmeldingKode(@JsonValue val kode: Int) {
    // Forespørsel om pasient
    FORESPØRSEL_OM_PASIENT(1),
    PÅMINNELSE_FORESPORSEL_OM_PASIENT(2),

    // Henvendelse fra NAV til lege
    RETUR_AV_LEGEERKLÆRING(3),
    MELDING_FRA_NAV(8),
}
