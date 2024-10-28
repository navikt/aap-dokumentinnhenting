package dokumentinnhenting.integrasjoner.syfo.bestilling

//TODO: Få inn navn på person her, ikke bare ident x2
fun genererBrev(dto: BehandlingsflytToDialogmeldingDTO): String {
    return when (dto.dokumentasjonType) {
        DokumentasjonType.L8 -> brev8L(dto.personIdent, dto.personIdent, dto.dialogmeldingTekst, dto.behandlerRef)
        DokumentasjonType.L40 -> brev40L(dto.personIdent, dto.personIdent, dto.dialogmeldingTekst, dto.behandlerRef)
        DokumentasjonType.L120 -> brev120()
        DokumentasjonType.MELDING_FRA_NAV -> brevMeldingFraNav(dto.personIdent, dto.personIdent, dto.dialogmeldingTekst, dto.behandlerRef)
        DokumentasjonType.RETUR_LEGEERKLÆRING -> brevReturLegeerklæring()
    }
}

fun brev8L(navn: String, fnr: String, fritekst: String, veileder: String): String {
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
        Med vennlig hilsen\n\n
        $veileder\n\n
        Nav
    """.trimIndent()
}
fun brev40L(navn: String, fnr: String, fritekst: String, veileder: String): String {
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
        Med vennlig hilsen\n
        $veileder\n
        Nav
    """.trimIndent()
}
fun brevMeldingFraNav(navn: String, fnr: String, fritekst: String, veileder: String): String {
    return """
        Melding fra Nav\n
        Gjelder pasient: $navn, $fnr.\n
        $fritekst\n
        Med vennlig hilsen\n
        $veileder\n
        Nav
    """.trimIndent()
}
fun brevReturLegeerklæring(): String {
    //TODO: Implement me
    return "Implement me"
}
fun brev120(): String {
    //TODO: Implement me
    return "Implement me"
}