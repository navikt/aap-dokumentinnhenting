package no.nav.aap.dokumentinnhenting.kontrakt

public data class BegrensetJournalpostDto(
    val journalpostId: String?,
    val dokumenter: List<BegrensetDokumentInfoDto>,
    val avsenderMottaker: AvsenderMottaker?,
)