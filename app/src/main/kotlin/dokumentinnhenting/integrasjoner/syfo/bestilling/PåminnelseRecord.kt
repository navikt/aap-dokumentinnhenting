package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.time.LocalDateTime
import java.util.UUID

data class PåminnelseRecord(
    val id: Long,
    val dialogmeldingId: UUID,
    val behandlingsReferanse: UUID,
    val status: PåminnelseStatus,
    val påminnelseDato: LocalDateTime,
    val opprettetTidspunkt: LocalDateTime,
    val sisteForespørselSendt: LocalDateTime?,
)

enum class PåminnelseStatus {
    PLANLAGT, AVBRUTT, SENDT
}