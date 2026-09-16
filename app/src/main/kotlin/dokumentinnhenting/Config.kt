package dokumentinnhenting

import no.nav.aap.komponenter.config.requiredConfigForKey
import java.util.*

data class Config(
    val dbConfig: DbConfig = DbConfig(),
)

data class DbConfig(
    val url: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_JDBC_URL"),
    val username: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_USERNAME"),
    val password: String = requiredConfigForKey("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_PASSWORD"),
)

object Azp {
    val ApiIntern: UUID = UUID.fromString(requiredConfigForKey("INTEGRASJON_API_INTERN_AZP"))
    val Behandlingsflyt: UUID = UUID.fromString(requiredConfigForKey("INTEGRASJON_BEHANDLINGSFLYT_AZP"))
}
