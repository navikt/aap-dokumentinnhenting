package dokumentinnhenting.util.dokument

import dokumentinnhenting.integrasjoner.saf.Doc
import java.time.LocalDateTime

fun List<Doc>.dokumentFilterDokumentSøk(): List<Doc> {
    return this.filter { doc ->
        sykemelding39Uker(doc)
            || dialogmeldingJournalførtPåAAPYngreEnnEttÅr(doc)
            || dialogmeldingJournalførtPåAAPEldreEnEttÅr(doc)
            || sykemeldingYngreEnnEttÅr(doc)
            || legeerklæring(doc)
    }
}

private fun dialogmeldingJournalførtPåAAPEldreEnEttÅr(dokument: Doc): Boolean {
    val brevkoderDialogmeldingAAP = listOf("900007", "900006")
    return dokument.brevkode in brevkoderDialogmeldingAAP
        && eldreEnnEttÅr(dokument.datoOpprettet)
}

private fun dialogmeldingJournalførtPåAAPYngreEnnEttÅr(dokument: Doc): Boolean {
    val brevkoderDialogmeldingAAP = listOf("900007", "900006")
    return dokument.brevkode in brevkoderDialogmeldingAAP
        && yngreEnnEttÅr(dokument.datoOpprettet)
}


private fun sykemeldingYngreEnnEttÅr(dokument: Doc): Boolean {
    return dokument.brevkode == "NAV 08-07.04 A"
        && yngreEnnEttÅr(dokument.datoOpprettet)
}

private fun legeerklæring(dokument: Doc): Boolean {
    val brevkoderLegeerklæring = listOf("NAV 08-07.08", "L9")
    val relevanteTema = listOf("AAP", "OPP", "SYK")

    return dokument.brevkode in brevkoderLegeerklæring && dokument.tema in relevanteTema
}

private fun sykemelding39Uker(dokument: Doc): Boolean {
    return dokument.brevkode == "NAV 08-07.04 R"
        && dokument.datoOpprettet > LocalDateTime.now().minusMonths(3)
}

private fun eldreEnnEttÅr(oprettet: LocalDateTime): Boolean {
    return oprettet < LocalDateTime.now().minusYears(1)
}

private fun yngreEnnEttÅr(opprettet: LocalDateTime): Boolean {
    return opprettet > LocalDateTime.now().minusYears(1)
}