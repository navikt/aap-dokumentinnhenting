package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.time.LocalDateTime

fun genererBrev(dto: BrevGenerering): String {
    return when (dto.dokumentasjonType) {
        DokumentasjonType.L8 -> brev8L(dto.personNavn, dto.personIdent, dto.dialogmeldingTekst)
        DokumentasjonType.L40 -> brev40L(dto.personNavn, dto.personIdent, dto.dialogmeldingTekst)
        DokumentasjonType.L120 -> brev120()
        DokumentasjonType.MELDING_FRA_NAV -> brevMeldingFraNav(dto.personNavn, dto.personIdent, dto.dialogmeldingTekst)
        DokumentasjonType.RETUR_LEGEERKLÆRING -> brevReturLegeerklæring(dto.personNavn, dto.personIdent, dto.dialogmeldingTekst)
        DokumentasjonType.PURRING -> brevPurring(dto.personNavn, dto.personIdent, dto.tidligereBestillingDato)
    }
}

private fun brev8L(navn: String, fnr: String, fritekst: String): String {
    return """
        Spørsmål om tilleggsopplysninger vedrørende pasient\n
        Gjelder pasient: $navn, $fnr.\n
        Nav trenger opplysninger fra deg vedrørende din pasient. Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.\n
        $fritekst\n
        Spørsmålene besvares i fritekst, og honoreres med takst L8.\n
        Lovhjemmel\n
        Folketrygdloven § 21-4 andre ledd gir Nav rett til å innhente nødvendige opplysninger. Dette gjelder selv om opplysningene er taushetsbelagte, jf. § 21-4 sjette ledd.\n\n
        Pålegget om utlevering av opplysninger kan påklages etter forvaltningsloven § 14.\n
        Klageadgangen gjelder kun lovligheten i pålegget. Fristen for å klage er tre dager etter at pålegget er mottatt. Klagen kan fremsettes muntlig eller skriftlig.\n\n
    """.trimIndent()
}

private fun brev40L(navn: String, fnr: String, fritekst: String): String {
    return """
        Forespørsel om legeerklæring ved arbeidsuførhet\n
        Gjelder pasient: $navn, $fnr.\n
        Nav trenger opplysninger fra deg vedrørende din pasient. Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.\n
        «Legeerklæring ved arbeidsuførhet» leveres på blankett Nav 08-07.08, og honoreres med takst L40.\n
        $fritekst\n
        Lovhjemmel\n
        Folketrygdloven § 21-4 andre ledd gir Nav rett til å innhente nødvendige opplysninger. Dette gjelder selv om opplysningene er taushetsbelagte, jf. § 21-4 sjette ledd.\n
        Pålegget om utlevering av opplysninger kan påklages etter forvaltningsloven § 14.\n
        Klageadgangen gjelder kun lovligheten i pålegget. Fristen for å klage er tre dager etter at pålegget er mottatt. Klagen kan fremsettes muntlig eller skriftlig.\n
    """.trimIndent()
}

private fun brevMeldingFraNav(navn: String, fnr: String, fritekst: String): String {
    return """
        Melding fra Nav\n
        Gjelder pasient: $navn, $fnr.\n
        $fritekst\n
    """.trimIndent()
}

private fun brevReturLegeerklæring(navn: String, fnr: String, fritekst: String): String {
    return """ 
        Retur av Legeerklæring ved arbeidsuførhet\n
        Gjelder $navn, $fnr.\n
        Vi har mottatt Legeerklæring ved arbeidsuførhet (NAV 08-07.08). Vi ber om at du sender oss en ny legeerklæring snarest mulig.\n
        Erklæringen kan ikke honoreres fordi den ikke inneholder tilstrekkelige opplysninger til bruk i den videre behandlingen av saken.\n
        $fritekst\n
        Hvis du har spørsmål til utfyllingen, henvises det til "Orientering til legen om bruk og utfylling av Legeerklæring ved arbeidsuførhet" (se nav.no).\n
        Dersom du allerede har sendt inn regning for den mangelfulle erklæringen, forutsetter vi at det ikke blir sendt regning for ny utfylt Legeerklæring ved arbeidsuførhet.\n
    """.trimIndent()
}

private fun brev120(): String {
    //TODO: Implement me
    return "Implement me brev120"
}

private fun brevPurring(navn: String, fnr: String, tidligereBestillingDato: LocalDateTime?): String {
    val tidligereDato = requireNotNull(tidligereBestillingDato).toLocalDate()
    return """
        Påminnelse om manglende svar vedrørerende pasient\n
        Gjelder $navn, f.nr. $fnr\n.
        Vi viser til tidligere forespørsel av $tidligereDato angående din pasient.\n
        Vi kan ikke se å ha mottatt svar på vår henvendelse og ber om at denne besvares snarest.\n
        Hvis opplysningene er sendt oss i løpet av de siste dagene, kan du se bort fra denne meldingen.\n
    """.trimIndent()
}

data class BrevPreviewResponse(
    val konstruertBrev: String
)

data class BrevGenerering(
    val personNavn: String,
    val personIdent: String,
    val dialogmeldingTekst: String,
    val dokumentasjonType: DokumentasjonType,
    val tidligereBestillingDato: LocalDateTime? = null,
)