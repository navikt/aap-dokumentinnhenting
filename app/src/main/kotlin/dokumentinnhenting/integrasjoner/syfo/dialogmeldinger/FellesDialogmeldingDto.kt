package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.saf.DokumentInfo
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import java.time.LocalDateTime

data class FellesDialogmeldingDto(
    val innkommendeUtgaaende: InnkommendeUtgaaende,
    val meldingFraNavn: String,
    val opprettetTidspunkt: LocalDateTime,
    val dokumentasjonsType: DokumentasjonType?,
    val tekst: String?,
    val meldingStatus: DialogmeldingLeveringStatus?,
    val journalpostId: String?,
    val dokumentIdListe: MutableList<DokumentInfo>
)