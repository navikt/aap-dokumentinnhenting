package tilgang.regler

internal sealed interface Regel<T> {
    fun vurder(input: T): Boolean
}

internal sealed interface InputGenerator<T> {
    suspend fun generer(input: RegelInput): T
}

internal class RegelMedInputgenerator<T>(val regel: Regel<T>, val inputGenerator: InputGenerator<T>) {
    suspend fun vurder(input: RegelInput): Boolean {
        val regelInput = inputGenerator.generer(input)
        return regel.vurder(regelInput)
    }
}