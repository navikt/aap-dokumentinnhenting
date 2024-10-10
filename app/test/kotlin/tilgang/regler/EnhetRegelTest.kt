package tilgang.regler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tilgang.enhet.EnhetRolle

class EnhetRegelTest {
    @Test
    fun `Saksenhet må ligge i saksbehandlers liste over enhetsroller`() {
        val roller = listOf(EnhetRolle("1234"), EnhetRolle("5678"))

        assertTrue(EnhetRegel.vurder(EnhetInput(roller, "1234")))
        assertFalse(EnhetRegel.vurder(EnhetInput(roller, "0000")))
    }
}