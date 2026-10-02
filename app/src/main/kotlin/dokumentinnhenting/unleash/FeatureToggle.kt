package dokumentinnhenting.unleash

interface FeatureToggle {
    fun key(): String
}

enum class FeatureToggles : FeatureToggle {
    // Feature toggles opprettes i Unleash: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    TestToggle,
    ;

    override fun key(): String = name
}