package dokumentinnhenting.integrasjoner.syfo.bestilling

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DialogmeldingBrevGeneratorTest {
    @Test
    fun `pasienten mottar kopi bare når varsling er aktivert`() {
        val medVarsling = genererDialogmelding(brevGenerering(skalVarsleBruker = true))
        val utenVarsling = genererDialogmelding(brevGenerering(skalVarsleBruker = false))

        assertThat(medVarsling).contains("Pasienten mottar en kopi av brevet.")
        assertThat(utenVarsling).doesNotContain("Pasienten mottar en kopi av brevet.")
    }

    private fun brevGenerering(skalVarsleBruker: Boolean) = BrevGenerering(
        personNavn = "Test Person",
        personIdent = "12345678901",
        dialogmeldingTekst = "",
        dokumentasjonType = DokumentasjonType.PURRING,
        tidligereBestillingDato = LocalDateTime.parse("2026-01-01T00:00:00"),
        skalVarsleBruker = skalVarsleBruker,
        signatur = null
    )
}
