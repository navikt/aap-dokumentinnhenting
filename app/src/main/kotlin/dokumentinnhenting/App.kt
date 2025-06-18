package dokumentinnhenting

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dokumentinnhenting.api.dokumentApi
import dokumentinnhenting.api.actuator
import dokumentinnhenting.api.syfoApi
import dokumentinnhenting.api.testApi
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytException
import dokumentinnhenting.integrasjoner.syfo.kafkaStreams
import dokumentinnhenting.util.metrics.prometheus
import dokumentinnhenting.util.motor.ProsesseringsJobber
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.retry.RetryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import no.nav.aap.komponenter.config.configForKey

internal val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
internal val logger: Logger = LoggerFactory.getLogger("app")

class App

private const val ANTALL_WORKERS = 4

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }
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

    install(StatusPages) {
        val logger = LoggerFactory.getLogger(App::class.java)
        exception<BehandlingsflytException> { call, cause ->
            logger.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause)
            call.respondText(
                text = "Feil i behandlingsflyt: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause)
            call.respondText(text = "Feil i tjeneste: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

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
        jobber = ProsesseringsJobber.alle()
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

fun initDatasource(dbConfig: DbConfig, meterRegistry: MeterRegistry) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"
    metricRegistry = meterRegistry
})
