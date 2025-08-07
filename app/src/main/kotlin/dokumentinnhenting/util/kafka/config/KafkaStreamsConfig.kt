package dokumentinnhenting.util.kafka.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringSerializer

import java.util.*

data class ProducerConfig(
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val schemaRegistry: SchemaRegistryConfig? = SchemaRegistryConfig(),
    val ssl: SslConfig? = SslConfig(),
    // val compressionType: String = "snappy", - DISABLED
) {
    fun properties(): Properties = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        schemaRegistry?.let { putAll(it.properties()) }
        ssl?.let { putAll(it.properties()) }
        put(ProducerConfig.ACKS_CONFIG, "all")
        // put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType) - DISABLED
    }
}

data class StreamsConfig(
    val applicationId: String = requiredConfigForKey("KAFKA_STREAMS_APPLICATION_ID"),
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val ssl: SslConfig? = SslConfig(),
    val schemaRegistry: SchemaRegistryConfig? = SchemaRegistryConfig(),
    //val compressionType: String = "snappy", - DISABLED
    val additionalProperties: Properties = Properties(),
) {
    fun streamsProperties(): Properties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = applicationId
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers

        ssl?.let { putAll(it.properties()) }
        schemaRegistry?.let { putAll(it.properties()) }
        putAll(additionalProperties)

        /* Exception handler when leaving the stream, e.g. serialization */
        this[StreamsConfig.PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            ExitPointExceptionHandler::class.java.name

        /*  Exception handler when entering the stream, e.g. deserialization */
        this[StreamsConfig.DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            EntryPointExceptionHandler::class.java.name

        // Configuration for resilience
        this[StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG)] = "all"
        this[StreamsConfig.REPLICATION_FACTOR_CONFIG] = 3
        this[StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG] = 1

        // Configuration for decreaseing latency
        this[StreamsConfig.producerPrefix(ProducerConfig.BATCH_SIZE_CONFIG)] = 0 // do not batch
        this[StreamsConfig.producerPrefix(ProducerConfig.LINGER_MS_CONFIG)] = 0 // send immediately

        // Configuration for message size - DISABLED
        //this[StreamsConfig.producerPrefix(ProducerConfig.COMPRESSION_TYPE_CONFIG)] = compressionType

        /*
         * Enable exactly onces semantics:
         * 1. ack produce to sink topic
         * 2. update state in app (state store)
         * 3. commit offset for source topic
         *
         * commit.interval.ms is set to 100ms
         * comsumers are configured with isolation.level="read_committed"
         * processing requires minimum three brokers
         */
        this[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = StreamsConfig.EXACTLY_ONCE_V2
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
    }
}

data class SslConfig(
    private val truststorePath: String = requiredConfigForKey("KAFKA_TRUSTSTORE_PATH"),
    private val keystorePath: String = requiredConfigForKey("KAFKA_KEYSTORE_PATH"),
    private val credstorePsw: String = requiredConfigForKey("KAFKA_CREDSTORE_PASSWORD"),
) {
    fun properties() = Properties().apply {
        this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        this[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        this[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
        this[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePsw
        this[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        this[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
        this[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePsw
        this[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePsw
        this[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
    }
}

data class SchemaRegistryConfig(
    private val url: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY"),
    private val user: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_USER"),
    private val password: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
) {
    fun properties() = Properties().apply {
        this["schema.registry.url"] = url
        this["basic.auth.credentials.source"] = "USER_INFO"
        this["basic.auth.user.info"] = "$user:$password"
    }
}

