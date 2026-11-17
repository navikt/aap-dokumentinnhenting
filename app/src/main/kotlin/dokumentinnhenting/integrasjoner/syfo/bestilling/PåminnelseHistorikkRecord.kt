package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.time.LocalDateTime

data class PåminnelseHistorikkRecord(
    val id: Long?,
    val påminnelseId: Long,
    val opprettetAv: String,
    val påminnelseStatus: PåminnelseStatus,
    val hendelseTidspunkt: LocalDateTime
)