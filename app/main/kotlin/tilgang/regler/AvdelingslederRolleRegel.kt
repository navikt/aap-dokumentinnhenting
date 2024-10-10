package tilgang.regler

import tilgang.Rolle

data object AvdelingslederRolleRegel: Regel<List<Rolle>> {
    override fun vurder(input: List<Rolle>): Boolean {
        return Rolle.AVDELINGSLEDER in input
    }
}