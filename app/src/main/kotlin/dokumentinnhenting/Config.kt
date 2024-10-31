package dokumentinnhenting

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig

data class Config (
    val DbConfig: DbConfig = DbConfig(),
    val azureConfig: AzureConfig = AzureConfig(),
)

data class DbConfig(
    val url: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_JDBC_URL"),
    val username: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_USERNAME"),
    val password: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_PASSWORD"),
)
