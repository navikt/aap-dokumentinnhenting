package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.util.*

data class BrevGenereringRequest(
    val bestillerNavIdent: String,
    val personNavn: String,
    val personIdent: String,
    val dialogmeldingTekst: String,
    val dokumentasjonType: DokumentasjonType,
    val tidligereBestillingReferanse: UUID? = null,
)