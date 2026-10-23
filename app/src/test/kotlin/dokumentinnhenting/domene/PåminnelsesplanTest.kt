package dokumentinnhenting.domene

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PåminnelsesplanTest {
    private val dato = LocalDate.of(2026, 10, 23)
    private val purringId = UUID.randomUUID()
    private val tilstander = listOf(
        Påminnelsestilstand.Planlagt,
        Påminnelsestilstand.Avbrutt,
        Påminnelsestilstand.Bortfalt(Bortfallsårsak.LEGEERKLÆRING_MOTTATT),
        Påminnelsestilstand.Utført(purringId),
    )

    @Test
    fun `bare planlagte påminnelser er forfalt på og etter datoen`() {
        for (tilstand in tilstander) {
            val plan = plan(tilstand)
            assertThat(listOf(dato.minusDays(1), dato, dato.plusDays(1)).map(plan::erForfalt))
                .isEqualTo(
                    if (tilstand == Påminnelsestilstand.Planlagt) listOf(false, true, true)
                    else listOf(false, false, false)
                )
        }
    }

    @Test
    fun `avbryt og gjenoppta bevarer planens data og dato`() {
        val plan = plan()
        val avbrutt = plan.avbryt()

        assertThat(avbrutt).usingRecursiveComparison()
            .isEqualTo(plan.copy(tilstand = Påminnelsestilstand.Avbrutt))
        assertThat(avbrutt.gjenoppta()).usingRecursiveComparison().isEqualTo(plan)
        assertThat(avbrutt.gjenoppta().erForfalt(dato.plusDays(3))).isTrue()
        assertThat(plan.tilstand).isEqualTo(Påminnelsestilstand.Planlagt)
    }

    @Test
    fun `planlagt og avbrutt plan kan bortfalle med en årsak`() {
        for (tilstand in listOf(Påminnelsestilstand.Planlagt, Påminnelsestilstand.Avbrutt)) {
            for (årsak in Bortfallsårsak.entries) {
                val plan = plan(tilstand)
                assertThat(plan.bortfall(årsak)).usingRecursiveComparison()
                    .isEqualTo(plan.copy(tilstand = Påminnelsestilstand.Bortfalt(årsak)))
            }
        }
    }

    @Test
    fun `utført plan refererer til opprettet purring`() {
        val plan = plan()

        assertThat(plan.registrerOpprettetPurring(purringId)).usingRecursiveComparison()
            .isEqualTo(plan.copy(tilstand = Påminnelsestilstand.Utført(purringId)))
    }

    @Test
    fun `ugyldige tilstandsoverganger avvises`() {
        for (tilstand in tilstander) {
            val plan = plan(tilstand)
            if (tilstand != Påminnelsestilstand.Planlagt) {
                assertThatIllegalStateException().isThrownBy { plan.avbryt() }
                assertThatIllegalStateException().isThrownBy { plan.registrerOpprettetPurring(purringId) }
            }
            if (tilstand != Påminnelsestilstand.Avbrutt) {
                assertThatIllegalStateException().isThrownBy { plan.gjenoppta() }
            }
            if (tilstand is Påminnelsestilstand.Utført || tilstand is Påminnelsestilstand.Bortfalt) {
                assertThatIllegalStateException()
                    .isThrownBy { plan.bortfall(Bortfallsårsak.LEGEERKLÆRING_MOTTATT) }
            }
        }
    }

    private fun plan(tilstand: Påminnelsestilstand = Påminnelsestilstand.Planlagt) = Påminnelsesplan(
        originalbestillingId = UUID.randomUUID(),
        behandlingsreferanse = BehandlingReferanse(UUID.randomUUID()),
        påminnelsesdato = dato,
        opprettetTidspunkt = LocalDateTime.of(2026, 10, 1, 10, 0),
        tilstand = tilstand,
    )
}
