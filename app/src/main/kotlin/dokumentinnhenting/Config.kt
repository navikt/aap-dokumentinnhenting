package dokumentinnhenting

import no.nav.aap.komponenter.config.requiredConfigForKey
import java.util.UUID

data class Config(
    val dbConfig: DbConfig = DbConfig(),
    val unleash: UnleashConfig = UnleashConfig(),
)

data class DbConfig(
    val url: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_JDBC_URL"),
    val username: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_USERNAME"),
    val password: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_PASSWORD"),
)

data class UnleashConfig(
    val apiUrl: String = requiredConfigForKey("UNLEASH_SERVER_API_URL"),
    val apiToken: String = requiredConfigForKey("UNLEASH_SERVER_API_TOKEN"),
    val environment: String = requiredConfigForKey("UNLEASH_SERVER_API_ENV"),
)

object Azp {
    val ApiIntern: UUID = UUID.fromString(requiredConfigForKey("INTEGRASJON_API_INTERN_AZP"))
    val Behandlingsflyt: UUID = UUID.fromString(requiredConfigForKey("INTEGRASJON_BEHANDLINGSFLYT_AZP"))
}

object ProdConfig {
    val config = Config()
}
