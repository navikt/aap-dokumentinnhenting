package dokumentinnhenting.integrasjoner.syfo.oppslag

import dokumentinnhenting.AzureTokenGen
import dokumentinnhenting.WithFakes
import dokumentinnhenting.randomPersonIdent
import dokumentinnhenting.randomNavIdent
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@WithFakes
class SyfoGatewayTest {

    val syfoGateway = SyfoGateway()

    @Test
    suspend fun `henter og cacher behandlere`() {
        val brukerIdent1 = randomPersonIdent()
        val brukerIdent2 = randomPersonIdent()
        val navIdent1 = randomNavIdent()
        val navIdent2 = randomNavIdent()
        val behandler1 = hentBehandlere(brukerIdent = brukerIdent1, navIdent = navIdent1)
        val behandler2 = hentBehandlere(brukerIdent = brukerIdent2, navIdent = navIdent1)

        assertThat(behandler1).isNotEqualTo(behandler2)

        // Tester caching så lenge fake-server gir random response
        assertThat(behandler1).isEqualTo(hentBehandlere(brukerIdent = brukerIdent1, navIdent = navIdent1))
        assertThat(behandler2).isEqualTo(hentBehandlere(brukerIdent = brukerIdent2, navIdent = navIdent1))

        // Tester caching så lenge fake-server gir random response
        assertThat(hentBehandlere(brukerIdent = brukerIdent1, navIdent = navIdent2)).isEqualTo(
            hentBehandlere(
                brukerIdent = brukerIdent1,
                navIdent = navIdent2
            )
        )
        assertThat(hentBehandlere(brukerIdent = brukerIdent2, navIdent = navIdent2)).isEqualTo(
            hentBehandlere(
                brukerIdent = brukerIdent2,
                navIdent = navIdent2
            )
        )

        // Tester at cache ikke deles på tvers av Nav-identer så lenge fake-server gir random response
        assertThat(behandler1).isNotEqualTo(hentBehandlere(brukerIdent = brukerIdent1, navIdent = navIdent2))
        assertThat(behandler2).isNotEqualTo(hentBehandlere(brukerIdent = brukerIdent2, navIdent = navIdent2))
    }

    private suspend fun hentBehandlere(brukerIdent: String, navIdent: String): List<BehandlerOppslagResponse> {
        val token =
            AzureTokenGen("dokumentinnhenting", "dokumentinnhenting").generate(navIdent = navIdent, isApp = false)
        return syfoGateway.behandlere(brukerIdent, OidcToken(token))
    }
}