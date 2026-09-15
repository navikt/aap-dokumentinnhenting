package no.nav.aap.dokumentinnhenting.kontrakt

import java.time.LocalDateTime
import java.util.UUID

public data class FellesDialogmeldingDto(
    val dialogmeldingReferanse: UUID?,
    val innkommendeUtgående: InnkommendeUtgående,
    val meldingFraNavn: String,
    val opprettetTidspunkt: LocalDateTime,
    val dokumentasjonsType: DokumentasjonType?,
    val tekst: String?,
    val meldingStatus: MeldingStatusDto?,
    val journalpostId: String?,
    val automatiskPåminnelse: Boolean? = null
)