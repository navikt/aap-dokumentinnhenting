package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.unleash.FakeUnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VarslingServiceTest {
    @Test
    fun `purring varsler bare når funksjonsbryteren er på`() {
        val deaktivert = VarslingService(FakeUnleashGateway(enabled = false))
        val aktivert = VarslingService(FakeUnleashGateway(enabled = true))

        assertThat(deaktivert.skalVarsleBruker(DokumentasjonType.PURRING)).isFalse()
        assertThat(aktivert.skalVarsleBruker(DokumentasjonType.PURRING)).isTrue()
    }

    @Test
    fun `forespørsler L8, L40 og L120 skal varsles til bruker`() {
        val tjenester = listOf(false, true).map { VarslingService(FakeUnleashGateway(enabled = it)) }

        tjenester.forEach { tjeneste ->
            assertThat(tjeneste.skalVarsleBruker(DokumentasjonType.L8)).isTrue()
            assertThat(tjeneste.skalVarsleBruker(DokumentasjonType.L40)).isTrue()
            assertThat(tjeneste.skalVarsleBruker(DokumentasjonType.L120)).isTrue()
            assertThat(tjeneste.skalVarsleBruker(DokumentasjonType.MELDING_FRA_NAV)).isFalse()
            assertThat(tjeneste.skalVarsleBruker(DokumentasjonType.RETUR_LEGEERKLÆRING)).isFalse()
        }
    }
}
