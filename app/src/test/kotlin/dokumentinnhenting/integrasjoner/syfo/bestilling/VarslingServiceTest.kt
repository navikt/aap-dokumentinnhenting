package dokumentinnhenting.integrasjoner.syfo.bestilling

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VarslingServiceTest {
    @Test
    fun `forespørsler L8, L40, L120 og purring skal varsles til bruker`() {
        val service = VarslingService()

        assertThat(service.skalVarsleBruker(DokumentasjonType.L8)).isTrue()
        assertThat(service.skalVarsleBruker(DokumentasjonType.L40)).isTrue()
        assertThat(service.skalVarsleBruker(DokumentasjonType.L120)).isTrue()
        assertThat(service.skalVarsleBruker(DokumentasjonType.PURRING)).isTrue()
        assertThat(service.skalVarsleBruker(DokumentasjonType.MELDING_FRA_NAV)).isFalse()
        assertThat(service.skalVarsleBruker(DokumentasjonType.RETUR_LEGEERKLÆRING)).isFalse()
    }
}
