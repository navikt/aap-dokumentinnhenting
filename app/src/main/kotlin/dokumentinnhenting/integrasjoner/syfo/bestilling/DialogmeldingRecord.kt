package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import java.time.LocalDateTime
import java.util.UUID

data class DialogmeldingRecord (
    val dialogmeldingUuid: UUID,
    val behandlerRef: String,
    val personIdent: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val behandlerNavn: String,
    val veilederNavn: String,
    val fritekst: String,
    val behandlingsReferanse: UUID
)

data class DialogmeldingFullRecord (
    val dialogmeldingUuid: UUID,
    val behandlerRef: String,
    val behandlerNavn: String,
    val veilederNavn: String,
    val personIdent: String,
    val dokumentasjonType: DokumentasjonType,
    val fritekst: String,
    val saksnummer: String,
    val status: MeldingStatusType?,
    val flytStatus: ProsesseringSyfoStatus?,
    val personNavn: String,
    val statusTekst: String?,
    val behandlingsReferanse: UUID,
    val opprettet: LocalDateTime
)