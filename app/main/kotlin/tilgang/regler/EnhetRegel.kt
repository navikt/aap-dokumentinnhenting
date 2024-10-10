package tilgang.regler

import tilgang.enhet.EnhetRolle
import tilgang.enhet.EnhetService

data object EnhetRegel : Regel<EnhetInput> {
    override fun vurder(input: EnhetInput): Boolean {
        return input.saksEnhet in input.enhetsRoller.map { it.kode }
    }
}

class EnhetInputGenerator(
    private val enhetService: EnhetService,
) : InputGenerator<EnhetInput> {
    override suspend fun generer(input: RegelInput): EnhetInput {
        val enhetsRoller = enhetService.hentEnhetRoller(input.currentToken, input.ansattIdent)
        return EnhetInput(enhetsRoller, "placeholder")
    }
}

data class EnhetInput(
    val enhetsRoller: List<EnhetRolle>,
    val saksEnhet: String
)
