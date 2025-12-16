package dokumentinnhenting

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dokumentinnhenting.api.actuator
import dokumentinnhenting.api.dokumentApi
import dokumentinnhenting.api.driftApi
import dokumentinnhenting.api.syfoApi
import dokumentinnhenting.api.testApi
import dokumentinnhenting.integrasjoner.syfo.kafkaStreams
import dokumentinnhenting.util.metrics.prometheus
import dokumentinnhenting.util.motor.ProsesseringsJobber
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry
import javax.sql.DataSource
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.json.DefaultJsonMapper.objectMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.retry.RetryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
internal val logger: Logger = LoggerFactory.getLogger("app")

private const val ANTALL_WORKERS = 4

fun main() {
    Thread.currentThread()
        .setUncaughtExceptionHandler { _, e ->
            SECURE_LOGGER.error(
                "Uhåndtert feil av type ${e.javaClass}.",
                e
            )
        }
    embeddedServer(Netty, configure = {
        connector {
            port = configForKey("PORT")?.toInt() ?: 8080
        }
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }, module = Application::server).start(wait = true)
}

fun Application.server(
    config: Config = Config(),
) {
    val prometheus = prometheus
    commonKtorModule(
        prometheus,
        AzureConfig(),
        InfoModel(title = "AAP - Dokumentinnhenting")
    )

    install(StatusPages, StatusPagesConfigHelper.setup())

    val dataSource = initDatasource(config.DbConfig, prometheus)
    Migrering.migrate(dataSource)
    val motor = module(dataSource)

    kafkaStreams(prometheus, dataSource)

    routing {
        actuator(prometheus, motor)

        authenticate(AZURE) {
            apiRouting {
                dokumentApi()
                motorApi(dataSource)
                syfoApi(dataSource)

                driftApi()
                if (Miljø.erDev()) {
                    testApi(dataSource)
                }
            }
        }
    }
}

fun Application.module(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = NoExtraLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = prometheus,
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}

fun initDatasource(dbConfig: DbConfig, meterRegistry: MeterRegistry) =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.username
        password = dbConfig.password
        maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = meterRegistry
    })

internal val defaultHttpClient = HttpClient(CIO) {
    expectSuccess = true

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper()))
    }
    install(HttpRequestRetry) {
        retryOnException(maxRetries = 3) // on exceptions during network send, other than timeouts
        exponentialDelay()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5_000
    }
}
