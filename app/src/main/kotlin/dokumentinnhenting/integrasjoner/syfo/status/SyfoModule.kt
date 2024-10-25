package dokumentinnhenting.integrasjoner.syfo.status

import dokumentinnhenting.util.kafka.KafkaStream
import io.ktor.server.application.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import dokumentinnhenting.util.kafka.NoopStream
import dokumentinnhenting.util.kafka.Stream
import dokumentinnhenting.util.kafka.config.StreamsConfig
import javax.sql.DataSource

fun Application.dialogmeldingStatusStream(registry: MeterRegistry, dataSource: DataSource): Stream {
  if (Miljø.er() == MiljøKode.LOKALT) return NoopStream()
  val config = StreamsConfig()
  val stream = KafkaStream(DialogmeldingStatusStream(dataSource).topology, config, registry)
  stream.start()

  monitor.subscribe(ApplicationStopped) {
    stream.close()
  }
  return stream
}