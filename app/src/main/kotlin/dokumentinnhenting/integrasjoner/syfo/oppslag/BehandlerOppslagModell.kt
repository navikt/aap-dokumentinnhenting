package dokumentinnhenting.integrasjoner.syfo.oppslag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse

@JsonIgnoreProperties(ignoreUnknown = true)
data class BehandlerOppslagResponse(
    val behandlerRef: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val kontor: String?,
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