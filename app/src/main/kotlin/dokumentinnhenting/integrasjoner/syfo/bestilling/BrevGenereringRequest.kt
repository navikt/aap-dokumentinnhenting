package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.util.*

data class BrevGenereringRequest(
    val personNavn: String,
    val personIdent: String,
    val dialogmeldingTekst: String,
    val veilederNavn: String,
    val dokumentasjonType: DokumentasjonType,
    val tidligeBestillingReferanse: UUID? = null,
)