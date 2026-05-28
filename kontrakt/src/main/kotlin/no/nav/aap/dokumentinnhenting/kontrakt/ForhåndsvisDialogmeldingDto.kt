package no.nav.aap.dokumentinnhenting.kontrakt

import java.util.UUID

data class ForhåndsvisDialogmeldingDto(
    val bestillerNavIdent: String,
    val personNavn: String,
    val personIdent: String,
    val dialogmeldingTekst: String,
    val dokumentasjonType: DokumentasjonType,
    val tidligereBestillingReferanse: UUID? = null,
)