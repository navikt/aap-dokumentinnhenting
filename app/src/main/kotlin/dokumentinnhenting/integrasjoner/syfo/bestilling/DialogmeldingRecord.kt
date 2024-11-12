package dokumentinnhenting.integrasjoner.syfo.bestilling

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