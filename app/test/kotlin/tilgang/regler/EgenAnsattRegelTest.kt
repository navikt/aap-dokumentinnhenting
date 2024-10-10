package tilgang.regler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import tilgang.Rolle

class EgenAnsattRegelTest {

    @Test
    fun `Egen ansatt med SKJERMET-rolle skal ha tilgang`() {
        val input = EgenAnsattInput(true, listOf(Rolle.KAN_BEHANDLE_SKJERMET))
        assertTrue(EgenAnsattRegel.vurder(input))
    }

    @Test
    fun `Egen ansatt uten SKJERMET-rolle skal ikke ha tilgang`() {
        val input = EgenAnsattInput(true, listOf(Rolle.VEILEDER))
        assertTrue(!EgenAnsattRegel.vurder(input))
    }

    @Test
    fun `Ikke-ansatt skal ha tilgang`() {
        val input = EgenAnsattInput(false, listOf(Rolle.VEILEDER))
        assertTrue(EgenAnsattRegel.vurder(input))
    }

    @Test
    fun `Ikke-ansatt med SKJERMET-rolle skal ha tilgang`() {
        val input = EgenAnsattInput(false, listOf(Rolle.KAN_BEHANDLE_SKJERMET))
        assertTrue(EgenAnsattRegel.vurder(input))
    }
}