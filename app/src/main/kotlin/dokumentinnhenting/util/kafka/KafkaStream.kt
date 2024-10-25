package dokumentinnhenting.util.kafka

import dokumentinnhenting.util.kafka.config.ProcessingExceptionHandler
import dokumentinnhenting.util.kafka.config.StreamsConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology

class KafkaStream(topology: Topology, config: StreamsConfig, registry: MeterRegistry): Stream {
    val streams = KafkaStreams(topology, config.streamsProperties())

    init {
        streams.setUncaughtExceptionHandler(ProcessingExceptionHandler())
        KafkaStreamsMetrics(streams).bindTo(registry)
    }

    override fun ready(): Boolean = streams.state() in listOf(
        KafkaStreams.State.CREATED,
        KafkaStreams.State.REBALANCING,
        KafkaStreams.State.RUNNING
    )
    override fun live(): Boolean = streams.state() != KafkaStreams.State.ERROR
    override fun close() = streams.close()
    override fun start() = streams.start()
}