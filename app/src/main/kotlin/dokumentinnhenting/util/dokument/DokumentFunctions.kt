package dokumentinnhenting.util.dokument

import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.DokumentInfo
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.Journalposttype
import dokumentinnhenting.integrasjoner.saf.RelevantDato
import dokumentinnhenting.integrasjoner.saf.Variantformat

fun mapTilDokumentliste(journalpost: Journalpost): List<Doc> = journalpost.dokumenter.flatMap { dok ->
    dok.dokumentvarianter
        .filter { it.variantformat === Variantformat.ARKIV }
        .map { dokumentvariant ->
            Doc(
                journalpostId = journalpost.journalpostId,
                tema = journalpost.tema ?: "Ukjent",
                dokumentInfoId = dok.dokumentInfoId,
                tittel = dok.tittel,
                brevkode = dok.brevkode,
                variantformat = dokumentvariant.variantformat,
                erUtgående = journalpost.journalposttype == Journalposttype.U,
                datoOpprettet = journalpost.datoOpprettet
                    ?: journalpost.relevanteDatoer?.first { it.datotype == RelevantDato.Datotype.DATO_JOURNALFOERT }?.dato!!
            )
        }
}

fun List<Journalpost>.mapKunVariantformatArkiv() = this
    .mapNotNull {
        val dokumenter = it.dokumenter.filter(DokumentInfo::harVariantformatArkiv)

        if (dokumenter.isEmpty()) null
        else it.copy(dokumenter = dokumenter)
    }

private fun DokumentInfo.harVariantformatArkiv(): Boolean =
    this.dokumentvarianter.any { variant -> variant.variantformat == Variantformat.ARKIV }
