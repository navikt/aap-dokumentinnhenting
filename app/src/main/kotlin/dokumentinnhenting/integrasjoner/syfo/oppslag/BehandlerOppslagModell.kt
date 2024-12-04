package dokumentinnhenting.integrasjoner.syfo.oppslag

data class BehandlerOppslagResponse(
    val type: String?,
    val behandlerRef: String,
    val fnr: String?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val orgnummer: String?,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
    val hprId: String?
)

data class FritekstRequest(
    val fritekst: String
)
