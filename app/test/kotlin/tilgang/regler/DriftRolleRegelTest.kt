package tilgang.regler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tilgang.Rolle

class DriftRolleRegelTest {
    @Test
    fun `Kun utvikler skal ha tilgang til operasjon 'DRIFTE'`() {
        assertTrue(DriftRolleRegel.vurder(listOf(Rolle.UTVIKLER)))
        assertFalse(
            DriftRolleRegel.vurder(
                listOf(
                    Rolle.LES,
                    Rolle.SAKSBEHANDLER,
                    Rolle.VEILEDER,
                    Rolle.AVDELINGSLEDER,
                    Rolle.BESLUTTER,
                    Rolle.FORTROLIG_ADRESSE,
                    Rolle.STRENGT_FORTROLIG_ADRESSE
                )
            )
        )
    }
}