package integrasjonportal.integrasjoner.syfo.status

import integrasjonportal.util.kafka.KafkaStream
import io.ktor.server.application.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import integrasjonportal.util.kafka.NoopStream
import integrasjonportal.util.kafka.Stream
import integrasjonportal.util.kafka.config.StreamsConfig

fun Application.dialogmeldingStatusStream(registry: MeterRegistry): Stream {
  if (Miljø.er() == MiljøKode.LOKALT) return NoopStream()
  val config = StreamsConfig()
  val stream = KafkaStream(DialogmeldingStatusStream().topology, config, registry)
  stream.start()

  monitor.subscribe(ApplicationStopped) {
    stream.close()
  }
  return stream
}