package no.nav.aap.dokumentinnhenting.kontrakt

import java.time.LocalDate
import java.time.LocalDateTime

public data class PåminnelseInfoResponse(
    val påminnelser: List<PåminnelseDto>
)

public data class PåminnelseDto(
    val status: PåminnelseStatusResponse,
    val påminnelseDato: LocalDate,
    val opprettetTidspunkt: LocalDateTime,
)

public enum class PåminnelseStatusResponse {
    PLANLAGT,
    SENDT,
    AVBRUTT
}