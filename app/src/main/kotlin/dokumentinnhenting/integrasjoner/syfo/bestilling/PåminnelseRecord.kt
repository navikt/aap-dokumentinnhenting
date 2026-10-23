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
)

enum class PåminnelseStatus {
    PLANLAGT, AVBRUTT, SENDT
}