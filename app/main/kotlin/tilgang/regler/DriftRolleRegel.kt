package tilgang.regler

import tilgang.Rolle

data object DriftRolleRegel : Regel<List<Rolle>> {
    override fun vurder(input: List<Rolle>): Boolean {
        return Rolle.UTVIKLER in input
    }
}