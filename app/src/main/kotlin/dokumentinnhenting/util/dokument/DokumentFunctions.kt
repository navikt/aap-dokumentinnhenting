package dokumentinnhenting.util.dokument

import dokumentinnhenting.api.tilKontrakt
import dokumentinnhenting.integrasjoner.saf.BegrensetJournalpostDto
import dokumentinnhenting.integrasjoner.saf.Doc
import dokumentinnhenting.integrasjoner.saf.DokumentInfo
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.Journalposttype
import dokumentinnhenting.integrasjoner.saf.RelevantDato
import dokumentinnhenting.integrasjoner.saf.Variantformat
import no.nav.aap.dokumentinnhenting.kontrakt.AvsenderMottakerDto
import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetDokumentInfoDto

fun mapTilDokumentliste(journalpost: Journalpost): List<Doc> = journalpost.dokumenter.flatMap { dok ->
    dok.dokumentvarianter
        .filter { it.variantformat === Variantformat.ARKIV }
        .map { dokumentvariant ->
            Doc(
                journalpostId = journalpost.journalpostId,
                tema = journalpost.tema ?: "Ukjent",
                dokumentInfoId = dok.dokumentInfoId,
                tittel = dok.tittel ?: "Mangler tittel",
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

fun BegrensetJournalpostDto.tilApi(): no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto {
    return no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto(
        journalpostId = this.journalpostId,
        dokumenter = this.dokumenter.map {
            dokumentDto -> BegrensetDokumentInfoDto(
                dokumentInfoId = dokumentDto.dokumentInfoId,
                tittel = dokumentDto.tittel,
            )
        },
        avsenderMottakerDto = AvsenderMottakerDto(
            id = this.avsenderMottaker?.id,
            type = this.avsenderMottaker?.type?.tilKontrakt(),
            navn = this.avsenderMottaker?.navn
        )
    )
}