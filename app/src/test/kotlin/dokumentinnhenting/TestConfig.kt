package dokumentinnhenting

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import java.net.URI

internal object TestConfig {
    fun default(fakes: Fakes, dbconf:DbConfig?=null):Config {
        return Config(
            azureConfig = AzureConfig(
                clientId = "aap-oppslag",
                clientSecret = "very",
                tokenEndpoint = URI.create("http://localhost:${fakes.azurePort()}/token"),
                jwksUri = "http://localhost:${fakes.azurePort()}/jwks",
                issuer = "azure"
            ),
            DbConfig = dbconf?:DbConfig(
                url = "jdbc:postgresql://localhost:5000/test",
                username = "test",
                password = "test"
            )
        )
    }
}