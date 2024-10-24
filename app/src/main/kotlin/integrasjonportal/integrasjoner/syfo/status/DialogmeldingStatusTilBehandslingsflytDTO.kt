package integrasjonportal.integrasjoner.syfo.status

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

data class DialogmeldingStatusTilBehandslingsflytDTO(
    val dialogmeldingUuid: UUID,
    val status: MeldingStatusType?,
    val behandlerRef: String,
    val personId: String,
    val sakId: String,
    val bestillingUuid: UUID?,
)

data class HentDialogmeldingStatusDto(@PathParam("saksnummer") val saksnummer: String)
