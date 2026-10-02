package dokumentinnhenting.unleash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FakeUnleashGatewayTest {
    @Test
    fun `fake returnerer konfigurert verdi`() {
        assertThat(FakeUnleashGateway(enabled = true).isEnabled(FeatureToggles.TestToggle)).isTrue()
        assertThat(FakeUnleashGateway(enabled = false).isEnabled(FeatureToggles.TestToggle)).isFalse()
    }

    @Test
    fun `feature toggle key er lik enum-navnet`() {
        assertThat(FeatureToggles.TestToggle.key()).isEqualTo("TestToggle")
    }
}