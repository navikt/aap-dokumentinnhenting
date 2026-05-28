package no.nav.aap.dokumentinnhenting.kontrakt

import java.time.LocalDateTime
import java.util.UUID

data class DialogmeldingStatusTilBehandslingsflytDto(
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
) {
    enum class MeldingStatusType() {
        BESTILT, SENDT, OK, AVVIST, MOTTATT
    }
}