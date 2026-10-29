package no.nav.aap.dokumentinnhenting.kontrakt

import java.util.UUID

public data class PåminnelseRequest(
    val dialogmeldingUuid: UUID
)

public data class AvbrytPåminnelseRequest(val dialogmeldingUuid: UUID)

public data class GjenopptaAutomatiskPåminnelseRequest(val dialogmeldingUuid: UUID)