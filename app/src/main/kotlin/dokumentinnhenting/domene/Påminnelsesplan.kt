package dokumentinnhenting.domene

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Påminnelsesplan(
    val originalbestillingId: UUID,
    val behandlingsreferanse: BehandlingReferanse,
    val påminnelsesdato: LocalDate,
    val opprettetTidspunkt: LocalDateTime,
    val tilstand: Påminnelsestilstand,
) {
    fun erForfalt(dato: LocalDate): Boolean =
        tilstand == Påminnelsestilstand.Planlagt && !dato.isBefore(påminnelsesdato)

    fun avbryt(): Påminnelsesplan {
        check(tilstand == Påminnelsestilstand.Planlagt) {
            "Bare en planlagt påminnelse kan avbrytes"
        }
        return copy(tilstand = Påminnelsestilstand.Avbrutt)
    }

    fun gjenoppta(): Påminnelsesplan {
        check(tilstand == Påminnelsestilstand.Avbrutt) {
            "Bare en avbrutt påminnelse kan gjenopptas"
        }
        return copy(tilstand = Påminnelsestilstand.Planlagt)
    }

    fun bortfall(årsak: Bortfallsårsak): Påminnelsesplan {
        check(tilstand == Påminnelsestilstand.Planlagt || tilstand == Påminnelsestilstand.Avbrutt) {
            "Bare en planlagt eller avbrutt påminnelse kan bortfalle"
        }
        return copy(tilstand = Påminnelsestilstand.Bortfalt(årsak))
    }

    fun registrerOpprettetPurring(purringId: UUID): Påminnelsesplan {
        check(tilstand == Påminnelsestilstand.Planlagt) {
            "Bare en planlagt påminnelse kan utføres"
        }
        return copy(tilstand = Påminnelsestilstand.Utført(purringId))
    }
}

sealed interface Påminnelsestilstand {
    data object Planlagt : Påminnelsestilstand
    data object Avbrutt : Påminnelsestilstand

    data class Bortfalt(val årsak: Bortfallsårsak) : Påminnelsestilstand
    data class Utført(val purringId: UUID) : Påminnelsestilstand
}

enum class Bortfallsårsak {
    LEGEERKLÆRING_MOTTATT,
    BEHANDLING_IKKE_LENGER_AKTUELL,
    PURRING_FINNES_ALLEREDE,
}
