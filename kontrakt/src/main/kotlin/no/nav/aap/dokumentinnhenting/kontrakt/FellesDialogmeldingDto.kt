package no.nav.aap.dokumentinnhenting.kontrakt

import java.time.LocalDateTime

public data class FellesDialogmeldingDto(
    val `innkommendeUtgående`: `InnkommendeUtgående`,
    val meldingFraNavn: String,
    val opprettetTidspunkt: LocalDateTime,
    val dokumentasjonsType: DokumentasjonType?,
    val tekst: String?,
    val meldingStatus: MeldingStatusDto?,
    val journalpostId: String?
)