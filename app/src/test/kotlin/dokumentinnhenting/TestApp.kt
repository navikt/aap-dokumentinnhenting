package dokumentinnhenting

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

fun main() {
    val dbConfig = initDbConfig()
    Fakes.start()

    // Starter server
    embeddedServer(Netty, port = 8082) {
        server(
            Config(
                dbConfig = dbConfig
            )
        )
        module()

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

private fun Application.module() {
    // Setter opp virtuell sandkasse lokalt
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        Fakes.close()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}

fun postgreSQLContainer(): org.testcontainers.postgresql.PostgreSQLContainer {
    val postgres = org.testcontainers.postgresql.PostgreSQLContainer("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}