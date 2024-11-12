package dokumentinnhenting.integrasjoner.saf

import java.time.LocalDateTime

fun dokumentFilter(dokumenter: List<Doc>): List<Doc> {
    return dokumenter.filter { doc ->
        dialogmeldingJournalførtPåAAP(doc) || sykemelding(doc)
    }
}

fun dialogmeldingJournalførtPåAAP(dokument:Doc):Boolean {
    val brevkoderDialogmeldingAAP = listOf("900007", "900006")
    return dokument.brevkode in brevkoderDialogmeldingAAP
            && dokument.datoOpprettet > LocalDateTime.now().minusYears(1)

}

fun sykemelding(dokument: Doc): Boolean {
    return dokument.brevkode == "NAV 08-07.04 A" && dokument.datoOpprettet > LocalDateTime.now().minusYears(1)
}
