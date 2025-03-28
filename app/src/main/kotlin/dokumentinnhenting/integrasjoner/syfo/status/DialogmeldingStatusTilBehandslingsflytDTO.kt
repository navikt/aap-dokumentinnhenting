package dokumentinnhenting.integrasjoner.syfo.status

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.time.LocalDateTime
import java.util.*

data class DialogmeldingStatusTilBehandslingsflytDTO(
    val dialogmeldingUuid: UUID,
    val status: MeldingStatusType?,
    val statusTekst: String?,
    val behandlerRef: String,
    val behandlerNavn: String,
    val personId: String,
    val saksnummer: String,
    val opprettet: LocalDateTime,
    val behandlingsReferanse: UUID,
    val fritekst: String
)

data class HentDialogmeldingStatusDTO(@PathParam("saksnummer") val saksnummer: String)