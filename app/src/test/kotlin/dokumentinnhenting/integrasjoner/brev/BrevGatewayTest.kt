package dokumentinnhenting.integrasjoner.brev

import dokumentinnhenting.Fakes
import dokumentinnhenting.WithFakes
import dokumentinnhenting.randomNavIdent
import dokumentinnhenting.randomPersonIdent
import no.nav.aap.brev.kontrakt.Signatur
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrevGatewayTest {

    @Test
    suspend fun `hentSignaturForhåndsvisning henter signatur`() {
        val navIdent = randomNavIdent()
        val forventetSignatur = Signatur("Navn $navIdent", "Enhet $navIdent")

        Fakes.signaturResopnseForRequestNavIdent[navIdent] = forventetSignatur

        val response = BrevGateway().hentSignaturForhåndsvisning(randomPersonIdent(), navIdent)

        assertThat(response).isEqualTo(forventetSignatur)
    }

    @Test
    suspend fun `hentSignaturForhåndsvisning gir null dersom ingen signatur`() {
        val navIdent = randomNavIdent()

        Fakes.signaturResopnseForRequestNavIdent[navIdent] = null

        val response = BrevGateway().hentSignaturForhåndsvisning(randomPersonIdent(), randomNavIdent())

        assertThat(response).isNull()
    }
}
