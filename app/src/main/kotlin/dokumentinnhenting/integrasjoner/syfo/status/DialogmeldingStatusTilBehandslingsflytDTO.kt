package dokumentinnhenting.integrasjoner.syfo.status

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import java.time.LocalDateTime
import java.util.UUID

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
    val fritekst: String,
)

data class HentDialogmeldingStatusDTO(@param:PathParam("saksnummer") val saksnummer: String)

fun DialogmeldingFullRecord.tilDto(): DialogmeldingStatusTilBehandslingsflytDTO {
    return DialogmeldingStatusTilBehandslingsflytDTO(
        dialogmeldingUuid = this.dialogmeldingUuid,
        status = this.status,
        statusTekst = this.statusTekst,
        behandlerRef = this.behandlerRef,
        behandlerNavn = this.behandlerNavn,
        personId = this.personIdent,
        saksnummer = this.saksnummer,
        opprettet = this.opprettet,
        behandlingsReferanse = this.behandlingsReferanse,
        fritekst = this.fritekst
    )
}
