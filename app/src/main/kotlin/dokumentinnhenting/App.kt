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
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestilling
import dokumentinnhenting.routes.syfo
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.commonKtorModule

internal val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")

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
    }) { server(DbConfig()) }.start(wait = true)
}

fun Application.server(dbConfig: DbConfig
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    commonKtorModule(
        prometheus,
        AzureConfig(),
        InfoModel(title = "AAP - Dokumentinnhenting")
    )

    /*
    * Services
    * */
    //val dialogmeldingStatusStream = dialogmeldingStatusStream(prometheus)

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

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)
    //val motor = module(dataSource) // Todo: implementer motor

    routing {
        actuator(prometheus/*, dialogmeldingStatusStream*/)

        authenticate(AZURE) {
            apiRouting {
                syfo(BehandlerDialogmeldingBestilling(monitor, dataSource), dataSource)
            }
        }
    }
}

/*
fun Application.module(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = BehandlingsflytLogInfoProvider,
        jobber = ProsesseringsJobber.alle()
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    environment.monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}*/

class DbConfig(
    val url: String = requiredConfigForKey("DB_DOKUMENTINNHENTING_JDBC_URL"),
    val username: String = requiredConfigForKey("DB_DOKUMENTINNHENTING_USERNAME"),
    val password: String = requiredConfigForKey("DB_DOKUMENTINNHENTING_PASSWORD"),
)

fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"
})
