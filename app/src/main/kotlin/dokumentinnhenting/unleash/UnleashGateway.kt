package dokumentinnhenting.unleash

interface UnleashGateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
}