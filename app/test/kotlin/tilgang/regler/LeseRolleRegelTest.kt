package tilgang.regler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tilgang.Rolle

class LeseRolleRegelTest {
    @Test
    fun `Leseroller skal kunne lese saker i Kelvin`() {
        assertFalse(
            LeseRolleRegel.vurder(
                listOf(
                    Rolle.UTVIKLER,
                    Rolle.FORTROLIG_ADRESSE,
                    Rolle.STRENGT_FORTROLIG_ADRESSE
                )
            )
        )
        assertTrue(LeseRolleRegel.vurder(listOf(Rolle.LES)))
        assertTrue(LeseRolleRegel.vurder(listOf(Rolle.SAKSBEHANDLER)))
        assertTrue(LeseRolleRegel.vurder(listOf(Rolle.VEILEDER)))
    }
}