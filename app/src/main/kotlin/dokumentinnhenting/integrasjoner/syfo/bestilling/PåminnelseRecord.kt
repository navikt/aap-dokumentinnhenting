package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PåminnelseRecord(
    val id: Long,
    val dialogmeldingId: UUID,
    val behandlingsReferanse: UUID,
    val status: PåminnelseStatus,
    val påminnelseDato: LocalDate,
    val opprettetTidspunkt: LocalDateTime,
    val sisteForespørselSendt: LocalDateTime?,
) {
    val erForespørselSendt: Boolean
        get() = sisteForespørselSendt != null
    fun erForespørselSendtSisteAntallDager(antallDager: Long) =
        sisteForespørselSendt != null &&
        sisteForespørselSendt.plusDays(antallDager) > LocalDateTime.now()
    fun harPåminnelseDatoPassert() =
        påminnelseDato.isBefore(LocalDate.now())
}

enum class PåminnelseStatus {
    PLANLAGT, AVBRUTT, SENDT
}