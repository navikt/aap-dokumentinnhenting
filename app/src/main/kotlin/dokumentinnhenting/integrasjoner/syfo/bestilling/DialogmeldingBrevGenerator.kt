package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.time.LocalDateTime
import no.nav.aap.brev.kontrakt.Signatur
import java.time.LocalDate

fun genererDialogmelding(dto: BrevGenerering): String {
    return buildString {
        when (dto.dokumentasjonType) {
            DokumentasjonType.L8 -> brev8L(dto.personNavn, dto.personIdent, dto.dialogmeldingTekst)
            DokumentasjonType.L40 -> brev40L(dto.personNavn, dto.personIdent, dto.dialogmeldingTekst)
            DokumentasjonType.L120 -> brev120()
            DokumentasjonType.MELDING_FRA_NAV -> brevMeldingFraNav(
                dto.personNavn,
                dto.personIdent,
                dto.dialogmeldingTekst
            )

            DokumentasjonType.RETUR_LEGEERKLÆRING -> brevReturLegeerklæring(
                dto.personNavn,
                dto.personIdent,
                dto.dialogmeldingTekst
            )

            DokumentasjonType.PURRING -> brevPurring(dto)
        }.also { append(it) }

        if (dto.dokumentasjonType.skalVarsleBruker()) {
            append("\n\nPasienten mottar en kopi av brevet.")
        }
        if (dto.signatur != null) {
            append("\n\n\nMed vennlig hilsen\n${dto.signatur.navn}\n${dto.signatur.enhet}")
        }
    }
}

private fun brev8L(navn: String, fnr: String, fritekst: String): String {
    return """
        |Spørsmål om tilleggsopplysninger vedrørende pasient
        |
        |Gjelder pasient: $navn, $fnr.
        |
        |Nav trenger opplysninger fra deg vedrørende din pasient. Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.
        |
        |$fritekst
        |
        |Spørsmålene besvares i fritekst, og honoreres med takst L8.
        |
        |Lovhjemmel
        |
        |Folketrygdloven § 21-4 andre ledd gir Nav rett til å innhente nødvendige opplysninger. Dette gjelder selv om opplysningene er taushetsbelagte, jf. § 21-4 sjette ledd.
        |
        |Pålegget om utlevering av opplysninger kan påklages etter forvaltningsloven § 14.
        |
        |Klageadgangen gjelder kun lovligheten i pålegget. Fristen for å klage er tre dager etter at pålegget er mottatt. Klagen kan fremsettes muntlig eller skriftlig.
    """.trimMargin()
}

private fun brev40L(navn: String, fnr: String, fritekst: String): String {
    return """
        |Forespørsel om legeerklæring ved arbeidsuførhet
        |
        |Gjelder pasient: $navn, $fnr.
        |
        |Nav trenger opplysninger fra deg vedrørende din pasient for å behandle sak om arbeidsavklaringspenger (AAP).
        |
        |Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.
        |
        |Vi ber om svar så fort som mulig og innen tre uker fra datoen brevet eller melding er datert, for å kunne behandle din pasient sin sak. Nav kan gi forlenget frist.
        |
        |«Legeerklæring ved arbeidsuførhet» leveres på blankett Nav 08-07.08, og honoreres med takst L40.
        |
        |$fritekst
        |
        |Kontakt oss
        |
        |Hvis du som helsepersonell trenger kontakt med Nav, kan du ringe oss på lege- og behandlertelefonen, 55 55 33 36, tast 2. Telefonen er betjent klokken 09.00–15.00.
        |
        |Lovhjemmel
        |
        |Folketrygdloven § 21-4 andre ledd gir Nav rett til å innhente nødvendige opplysninger. Dette gjelder selv om opplysningene er taushetsbelagte, jf. § 21-4 sjette ledd.
        |
        |Pålegget om utlevering av opplysninger kan påklages etter forvaltningsloven § 14.
        |
        |Klageadgangen gjelder kun lovligheten i pålegget. Fristen for å klage er tre dager etter at pålegget er mottatt. Klagen kan fremsettes muntlig eller skriftlig.
    """.trimMargin()
}

private fun brevMeldingFraNav(navn: String, fnr: String, fritekst: String): String {
    return """
        |Melding fra Nav
        |
        |Gjelder pasient: $navn, $fnr.
        |
        |$fritekst
    """.trimMargin()
}

private fun brevReturLegeerklæring(navn: String, fnr: String, fritekst: String): String {
    return """
        |Retur av Legeerklæring ved arbeidsuførhet
        |
        |Gjelder $navn, $fnr.
        |
        |Vi har mottatt Legeerklæring ved arbeidsuførhet (NAV 08-07.08). Vi ber om at du sender oss en ny legeerklæring snarest mulig.
        |
        |Erklæringen kan ikke honoreres fordi den ikke inneholder tilstrekkelige opplysninger til bruk i den videre behandlingen av saken.
        |
        |$fritekst
        |
        |Hvis du har spørsmål til utfyllingen, henvises det til "Orientering til legen om bruk og utfylling av Legeerklæring ved arbeidsuførhet" (se nav.no).
        |
        |Dersom du allerede har sendt inn regning for den mangelfulle erklæringen, forutsetter vi at det ikke blir sendt regning for ny utfylt Legeerklæring ved arbeidsuførhet.
    """.trimMargin()
}

private fun brev120(): String {
    //TODO: Implement me
    return ""
}

private fun brevPurring(dto: BrevGenerering): String {
    val tidligereDato = requireNotNull(dto.tidligereBestillingDato).toLocalDate()
    return """
        |Til: ${dto.behandlerNavn}
        |
        |HPR-nummer: ${dto.behandlerHprNr}
        |
        |Dato: ${dto.forsendelseDato}
        |
        |Vår referanse: ${dto.saksnummer}
        |
        |Påminnelse på forespørsel om legeerklæring
        |
        |Gjelder pasient: ${dto.personNavn}, f.nr. ${dto.personIdent}
        |
        |Viser til tidligere forespørsel om legeerklæring sendt $tidligereDato. Vi har ikke mottatt svar og ber deg besvare denne snarest.
        |
        |Hvis du har sendt oss opplysningene i løpet av de siste dagene, kan du se bort fra denne.
    """.trimIndent()
}

data class BrevGenerering(
    val personNavn: String,
    val personIdent: String,
    val behandlerNavn: String?,
    val behandlerHprNr: String?,
    val dialogmeldingTekst: String,
    val dokumentasjonType: DokumentasjonType,
    val tidligereBestillingDato: LocalDateTime? = null,
    val forsendelseDato: LocalDate? = null,
    val saksnummer: String? = null,
    val signatur: Signatur?
)