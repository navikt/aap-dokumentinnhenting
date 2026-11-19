package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.util.NavIdent
import java.time.LocalDateTime

data class PåminnelseHistorikkRecord(
    val id: Long?,
    val påminnelseId: Long,
    val opprettetAv: NavIdent,
    val påminnelseStatus: PåminnelseStatus,
    val hendelseTidspunkt: LocalDateTime
)