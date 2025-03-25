package dokumentinnhenting.integrasjoner.syfo.oppslag

import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse

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
    val fritekst: String,
    val saksnummer: String
): Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}