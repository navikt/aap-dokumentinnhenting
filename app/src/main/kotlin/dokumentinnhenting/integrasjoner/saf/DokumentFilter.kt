package dokumentinnhenting.integrasjoner.saf

import java.time.LocalDateTime

fun dokumentFilterDokumentSøk(dokumenter: List<Doc>): List<Doc> {
    return dokumenter.filter { doc ->
        sykemelding39Uker(doc) || dialogmeldingJournalførtPåAAPYngreEnnEttÅr(doc) || dialogmeldingJournalførtPåAAPEldreEnEttÅr(doc) || sykemeldingYngreEnnEttÅr(doc) || Legeerklæring(doc)
    }
}

fun dokumentFilterAutomatiskJournalføring(dokumenter: List<Doc>): List<Doc>{
    return dokumenter.filter { doc ->
        dialogmeldingJournalførtPåAAPYngreEnnEttÅr(doc) || sykemeldingYngreEnnEttÅr(doc)
    }
}

fun dialogmeldingJournalførtPåAAPEldreEnEttÅr (dokument:Doc):Boolean {
    val brevkoderDialogmeldingAAP = listOf("900007", "900006")
    return dokument.brevkode in brevkoderDialogmeldingAAP
            && eldreEnnEttÅr(dokument.datoOpprettet)
}

fun dialogmeldingJournalførtPåAAPYngreEnnEttÅr (dokument:Doc):Boolean {
    val brevkoderDialogmeldingAAP = listOf("900007", "900006")
    return dokument.brevkode in brevkoderDialogmeldingAAP
            && yngreEnnEttÅr(dokument.datoOpprettet)
}


fun sykemeldingEldreEnnEttÅr(dokument: Doc): Boolean {
    return dokument.brevkode == "NAV 08-07.04 A"
            && eldreEnnEttÅr(dokument.datoOpprettet)
}

fun sykemeldingYngreEnnEttÅr(dokument: Doc): Boolean {
    return dokument.brevkode == "NAV 08-07.04 A"
            && yngreEnnEttÅr(dokument.datoOpprettet)
}

fun Legeerklæring(dokument: Doc): Boolean {
    val brevkoderLegeerklæring = listOf("NAV 08-07.08", "L9")
    val relevanteTema = listOf("AAP", "OPP", "SYK")
    return dokument.brevkode in brevkoderLegeerklæring
            && dokument.tema in relevanteTema

}

fun sykemelding39Uker(dokument: Doc): Boolean {
    return dokument.brevkode == "NAV 08-07.04 R"
            && dokument.datoOpprettet > LocalDateTime.now().minusMonths(3)
}


fun eldreEnnEttÅr(oprettet: LocalDateTime): Boolean {
    return oprettet < LocalDateTime.now().minusYears(1)
}

fun yngreEnnEttÅr(opprettet: LocalDateTime): Boolean {
    return opprettet > LocalDateTime.now().minusYears(1)
}