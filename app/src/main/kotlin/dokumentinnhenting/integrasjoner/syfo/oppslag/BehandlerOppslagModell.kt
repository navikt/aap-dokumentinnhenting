package dokumentinnhenting.integrasjoner.syfo.oppslag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlerDto
import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse

@JsonIgnoreProperties(ignoreUnknown = true)
data class BehandlerOppslagResponse(
    val type: String?,
    val behandlerRef: String,
    val kategori: String,
    val fnr: String?,
    val hprId: String?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val orgnummer: String?,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
) {
    fun tilDto(): BehandlerDto {
        return BehandlerDto(
            behandlerRef = behandlerRef,
            hprId = hprId,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            kontor = kontor,
            adresse = adresse,
            postnummer = postnummer,
            poststed = poststed,
            telefon = telefon,
        )
    }
}

data class FritekstRequest(
    val fritekst: String,
    val saksnummer: String
) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}

data class HentFastlegeDtoSaksreferanse(val saksnummer: String, val personIdent: String) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}