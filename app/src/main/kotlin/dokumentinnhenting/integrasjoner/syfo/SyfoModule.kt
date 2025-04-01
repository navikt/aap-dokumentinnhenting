package dokumentinnhenting.integrasjoner.syfo

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.DialogmeldingMedSaksknyttning
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.FiltrerDialogmeldingUtfører
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.HåndterMottattDialogmeldingUtfører
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.kafka.*
import io.ktor.server.application.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import dokumentinnhenting.util.kafka.config.StreamsConfig
import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("app")

const val SYFO_DIALOGMELDING_MOTTAK_TOPIC = "teamsykefravr.dialogmelding"
const val SYFO_STATUS_DIALOGMELDING_TOPIC = "teamsykefravr.behandler-dialogmelding-status"

fun Application.kafkaStreams(registry: MeterRegistry, dataSource: DataSource): Stream {
  if (Miljø.er() == MiljøKode.LOKALT) return NoopStream()

  val topology = createDialogmeldingStreamTopology(dataSource)
  val config = StreamsConfig()
  val stream = KafkaStream(topology, config, registry)

  stream.start()

  monitor.subscribe(ApplicationStopped) {
    stream.close()
  }

  return stream
}

fun createDialogmeldingStreamTopology(
  dataSource: DataSource
): Topology {
  val dialogmeldingStatusSerde = createGenericSerde(DialogmeldingStatusDTO::class.java)
  val dialogmeldingMottakSerde = createGenericSerde(DialogmeldingMottakDTO::class.java)

  val builder = StreamsBuilder()

  val combinedStream = builder.stream(
    listOf(SYFO_STATUS_DIALOGMELDING_TOPIC, SYFO_DIALOGMELDING_MOTTAK_TOPIC),
    Consumed.with(Serdes.String(), CustomSerde(dialogmeldingStatusSerde, dialogmeldingMottakSerde))
  )


  combinedStream.split()
    .branch(
      { _, value -> value is DialogmeldingStatusDTO },
      Branched.withConsumer { ks: KStream<String, Any> ->
        ks.mapValues { value -> value as DialogmeldingStatusDTO }
          .filter { _, record -> bestillingEksisterer(dataSource, record.bestillingUuid) }
          .foreach { _, record -> oppdaterStatus(dataSource, record) }
      }.withName("StatusStream")
    )
    .branch(
      { _, value -> value is DialogmeldingMottakDTO },
      Branched.withConsumer { ks: KStream<String, Any> ->
        ks.mapValues { value -> value as DialogmeldingMottakDTO }
          .foreach { _, record ->
            opprettJobb(dataSource, record)
          }
      }.withName("MottakStream")
    )
    .defaultBranch(
      Branched.withConsumer { ks: KStream<String, Any> ->
        ks.peek { _, value -> log.warn("Unknown message type: $value") }
      }.withName("Unknown")
    )

  return builder.build()
}




fun oppdaterStatus(dataSource: DataSource, record: DialogmeldingStatusDTO) {
  dataSource.transaction{ connection ->
    val jobbRepository = FlytJobbRepository(connection)
    val dialogmeldingRepository = DialogmeldingRepository(connection)

    log.info("Oppdaterer status på ${record.bestillingUuid} med status ${record.status}")

    val lagretBestilling = requireNotNull(dialogmeldingRepository.hentByDialogId(UUID.fromString(record.bestillingUuid)))

    val jobb =
      JobbInput(OppdaterLegeerklæringStatusUtfører)
        .medCallId()
        .medPayload(DefaultJsonMapper.toJson(record))
        .forSak(lagretBestilling.id)

    jobbRepository.leggTil(jobb)
  }
}

fun bestillingEksisterer(datasource: DataSource,bestillingUuid: String): Boolean {
  return datasource.transaction { connection ->
    val repository = DialogmeldingRepository(connection)
    val record = repository.hentByDialogId(UUID.fromString(bestillingUuid))

    record?.dialogmeldingUuid.toString() == bestillingUuid
  }
}

fun opprettJobb(dataSource: DataSource, dto: DialogmeldingMottakDTO) {
  dataSource.transaction { connection ->
    val flytJobbRepository = FlytJobbRepository(connection)

    flytJobbRepository.leggTil(
      JobbInput(FiltrerDialogmeldingUtfører).medPayload(
        DefaultJsonMapper.toJson(dto)
      )
    )
  }
}