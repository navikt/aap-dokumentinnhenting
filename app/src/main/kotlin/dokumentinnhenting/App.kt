package dokumentinnhenting

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import dokumentinnhenting.routes.actuator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.aap.komponenter.server.AZURE
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytException
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingMottakStream
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingStatusStream
import dokumentinnhenting.routes.syfo
import dokumentinnhenting.util.motor.ProsesseringsJobber
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.retry.RetryService
import saf
import javax.sql.DataSource

internal val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
internal val logger: Logger = LoggerFactory.getLogger("app")
class App

val SYSTEMBRUKER = Bruker("Kelvin")

private const val ANTALL_WORKERS = 4

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, configure = {
        connector {
            port = 8080
        }
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }, module = Application::server).start(wait = true)
}

fun Application.server(
    config: Config = Config(),
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    commonKtorModule(
        prometheus,
        AzureConfig(),
        InfoModel(title = "AAP - Dokumentinnhenting")
    )

    install(StatusPages) {
        val logger = LoggerFactory.getLogger(App::class.java)
        exception<BehandlingsflytException> { call, cause ->
            logger.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause.stackTraceToString())
            logger.error("Feil i behandlingsflyt: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(
                text = "Feil i behandlingsflyt: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause)
            logger.error("Feil i tjeneste: ${cause.message} \n ${cause.stackTraceToString()}")
            call.respondText(text = "Feil i tjeneste: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

    val dataSource = initDatasource(config.DbConfig)
    Migrering.migrate(dataSource)
    val motor = module(dataSource)

    dialogmeldingStatusStream(prometheus, dataSource)
    dialogmeldingMottakStream(prometheus, dataSource)

    routing {
        actuator(prometheus, motor)

        authenticate(AZURE) {
            apiRouting {
                saf()
                motorApi(dataSource)
                syfo(dataSource)
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

fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"
})
