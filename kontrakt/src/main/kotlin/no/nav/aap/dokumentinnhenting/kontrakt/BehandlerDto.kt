package no.nav.aap.dokumentinnhenting.kontrakt

data class BehandlerDto(
    val behandlerRef: String,
    val hprId: String?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
)
