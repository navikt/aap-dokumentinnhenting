package dokumentinnhenting.integrasjoner.syfo.oppslag

import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SyfoGatewayTest {

    val syfoGateway = SyfoGateway()

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Fakes.start()
        }
    }


    @Test
    suspend fun `henter og cacher behandlere`() {
        val brukerIdent1 = randomBrukerIdent()
        val brukerIdent2 = randomBrukerIdent()
        val behandler1 = hentBehandlere(brukerIdent1)
        val behandler2 = hentBehandlere(brukerIdent2)

        assertThat(behandler1).isNotEqualTo(behandler2)

        // Tester caching så lenge fake-server gir random response
        assertThat(behandler1).isEqualTo(hentBehandlere(brukerIdent1))
        assertThat(behandler2).isEqualTo(hentBehandlere(brukerIdent2))
    }

    private suspend fun hentBehandlere(brukerIdent: String): List<BehandlerOppslagResponse> {
        val token = AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate(isApp = false)
        return syfoGateway.behandlere(brukerIdent, OidcToken(token))
    }

    private fun randomBrukerIdent(): String {
        return Random.nextLong(10000000000L, 99999999999L).toString()
    }
}