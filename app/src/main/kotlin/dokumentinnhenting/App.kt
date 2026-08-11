package dokumentinnhenting

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dokumentinnhenting.api.actuator
import dokumentinnhenting.api.dialogmeldingApi
import dokumentinnhenting.api.dokumentApi
import dokumentinnhenting.api.driftApi
import dokumentinnhenting.api.syfoApi
import dokumentinnhenting.api.testApi
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.integrasjoner.dokarkiv.DokarkivGateway
import dokumentinnhenting.integrasjoner.syfo.kafkaStreams
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import dokumentinnhenting.prosessering.DokumentinnhentingLogInfoProvider
import dokumentinnhenting.util.kafka.config.ProducerConfig
import dokumentinnhenting.util.metrics.prometheus
import dokumentinnhenting.util.motor.ProsesseringsJobber
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
import kotlin.time.Duration.Companion.seconds
import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.json.DefaultJsonMapper.objectMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.tilgang.TeamAap
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")
internal val logger: Logger = LoggerFactory.getLogger("app")

private const val ANTALL_WORKERS = 4

lateinit var kafkaProducer: KafkaProducer<String, String>

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
    if (!::kafkaProducer.isInitialized) {
        kafkaProducer = KafkaProducer(ProducerConfig().properties())
    }

    val prometheus = prometheus
    commonKtorModule(
        prometheus,
        identityProvider = IdentityProvider.ENTRA_ID,
        infoModel = InfoModel(title = "AAP - Dokumentinnhenting")
    )

    install(StatusPages, StatusPagesConfigHelper.setup())

    val dataSource = initDatasource(config.dbConfig, prometheus)
    Migrering.migrate(dataSource)
    val motor = module(dataSource)

    kafkaStreams(prometheus, dataSource)

    val brevGateway = BrevGateway()
    val syfoGateway = SyfoGateway()
    val dokarkivGateway = DokarkivGateway(OboTokenProvider)
    val påkrevdeRollerMotor = if (Miljø.erProd()) listOf(TeamAap.id) else emptyList()

    routing {
        actuator(prometheus, motor)

        authenticate(IdentityProvider.ENTRA_ID.value) {
            apiRouting {
                motorApi(dataSource, påkrevdeRollerMotor)
                syfoApi(dataSource, brevGateway, syfoGateway)
                dokumentApi(dokarkivGateway)
                dialogmeldingApi(dataSource)

                driftApi(dataSource)

                if (Miljø.erDev()) {
                    testApi(dataSource, brevGateway)
                }
            }
        }
    }
}

fun Application.module(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = DokumentinnhentingLogInfoProvider,
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
        kafkaProducer.close()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
        defaultHttpClient.close()
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
    install(HttpTimeout) {
        connectTimeoutMillis = 10.seconds.inWholeMilliseconds
        requestTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
}
