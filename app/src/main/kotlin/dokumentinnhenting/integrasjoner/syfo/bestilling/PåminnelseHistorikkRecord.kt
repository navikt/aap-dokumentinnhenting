package dokumentinnhenting.integrasjoner.syfo.bestilling

import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

data class PåminnelseHistorikkRecord(
    val id: Long?,
    val påminnelseId: Long,
    val opprettetAv: Bruker,
    val påminnelseStatus: PåminnelseStatus,
    val sisteForespørselSendt: LocalDateTime?,
    val hendelseTidspunkt: LocalDateTime
)