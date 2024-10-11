package integrasjonportal.integrasjoner.syfo



import io.ktor.server.application.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import integrasjonportal.util.kafka.NoopStream
import integrasjonportal.util.kafka.Stream
import integrasjonportal.util.kafka.config.StreamsConfig
import io.ktor.server.engine.*

import javax.sql.DataSource

val topic = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"

/*
TODO Ta det på mandag
fun Application.mottakStream(dataSource: DataSource, registry: MeterRegistry): Stream {
  if (Miljø.er() == MiljøKode.LOKALT) return NoopStream()

  val config = StreamsConfig()
  val stream = DialogmeldingStream(JoarkKafkaHandler(config, dataSource).topology, config, registry) // TODO mangler denne
  stream.start()
 monitor.subscribe(ApplicationStopped) {
      stream.close()
  }
  return stream

}
*/
