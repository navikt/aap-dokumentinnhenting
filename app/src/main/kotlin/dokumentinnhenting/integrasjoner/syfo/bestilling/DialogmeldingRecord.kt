package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.util.UUID

data class DialogmeldingRecord (
    val dialogmeldingUuid: UUID,
    val behandlerRef: String,
    val personIdent: String,
    val sakId: String
)