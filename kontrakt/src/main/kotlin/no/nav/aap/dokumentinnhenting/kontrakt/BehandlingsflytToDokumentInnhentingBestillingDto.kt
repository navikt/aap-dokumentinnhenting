package no.nav.aap.dokumentinnhenting.kontrakt

import java.util.UUID

data class BehandlingsflytToDokumentInnhentingBestillingDto(
    val bestillerNavIdent: String,
    val behandlerRef: String,
    val behandlerNavn: String,
    val behandlerHprNr: String,
    val personIdent: String,
    val personNavn: String,
    val dialogmeldingTekst: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val behandlingsReferanse: UUID,
    val tidligereBestillingReferanse: UUID? = null
)
