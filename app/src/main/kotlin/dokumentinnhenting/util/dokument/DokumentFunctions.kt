package dokumentinnhenting.util.dokument

import dokumentinnhenting.integrasjoner.saf.Doc
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
                tema = journalpost.temanavn ?: journalpost.behandlingstemanavn ?: "Ukjent",
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
