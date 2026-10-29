package no.nav.aap.dokumentinnhenting.kontrakt

import java.util.UUID

@Deprecated("Disse vil fjernes")
public data class PåminnelseRequest(
    val dialogmeldingUuid: UUID, val behandlingReferanse: Any? = null,
)

@Deprecated("Disse vil fjernes")
public data class AvbrytPåminnelseRequest(val dialogmeldingUuid: UUID, val behandlingReferanse: Any? = null)

@Deprecated("Disse vil fjernes")
public data class GjenopptaAutomatiskPåminnelseRequest(
    val dialogmeldingUuid: UUID,
    val behandlingReferanse: Any? = null
)