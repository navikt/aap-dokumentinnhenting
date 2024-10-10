package tilgang

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tilgang.auth.AzureConfig
import java.net.URI

fun main() {
    val fakes = Fakes(azurePort = 8081)

    embeddedServer(Netty, port = 8080) {
        api(
            Config(
                azureConfig = AzureConfig(
                    clientId = "",
                    clientSecret = "",
                    tokenEndpoint = URI.create("http://localhost:${fakes.azurePort()}").resolve("/token").toURL(),
                    jwks = URI.create("http://localhost:${fakes.azurePort()}").resolve("/jwks").toURL(),
                    issuer = ""
                ),
                redis = RedisConfig(
                    uri = URI.create("http://127.0.0.1:6379"),
                    username = "test",
                    password = "test"
                ),
                roles = listOf(),
                // Kan fylles inn med ekte verdier om disse fakes i Fakes
                pdlConfig = PdlConfig(baseUrl = "", audience = "", scope = ""),
                msGraphConfig = MsGraphConfig("", ""),
                behandlingsflytConfig = BehandlingsflytConfig("", "")
            )
        )
    }.start(wait = true)
}


private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}