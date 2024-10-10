package tilgang.regler

import tilgang.Rolle
import tilgang.integrasjoner.skjerming.SkjermingClient

data object EgenAnsattRegel : Regel<EgenAnsattInput> {
    override fun vurder(input: EgenAnsattInput): Boolean {

        return when (input.finnesAnsattIFamilie){
            true -> input.roller.contains(Rolle.KAN_BEHANDLE_SKJERMET)
            false -> true
        }
    }
}

class EgenAnsattInputGenerator(private val skjermingClient: SkjermingClient) : InputGenerator<EgenAnsattInput> {
    override suspend fun generer(input: RegelInput): EgenAnsattInput {
        val finnesAnsattIFamilie = skjermingClient.isSkjermet(input.søkerIdenter)
        return EgenAnsattInput(finnesAnsattIFamilie, input.roller)
    }
}

data class EgenAnsattInput(val finnesAnsattIFamilie: Boolean, val roller: List<Rolle>)