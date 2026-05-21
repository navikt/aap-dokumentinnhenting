package dokumentinnhenting

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

fun main() {
    val dbConfig = initDbConfig()
    val fakes = Fakes

    // Starter server
    embeddedServer(Netty, port = 8082) {
        server(
            Config(
                dbConfig = dbConfig
            )
        )
        module(fakes)

        initDatasource(dbConfig, SimpleMeterRegistry())

    }.start(wait = true)
}

private fun initDbConfig(): DbConfig {
    return if (System.getenv("NAIS_DATABASE_DOKUMENTINNHENTING_DOKUMENTINNHENTING_JDBC_URL").isNullOrBlank()) {
        val postgres = postgreSQLContainer()

        DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
    } else {
        DbConfig()
    }.also {
        println("----\nDATABASE URL: \n${it.url}?user=${it.username}&password=${it.password}\n----")
    }
}

private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}

fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}