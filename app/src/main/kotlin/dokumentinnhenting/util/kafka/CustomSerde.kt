package dokumentinnhenting.util.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

fun <T> createGenericSerde(clazz: Class<T>): Serde<T> {
    val objectMapper = ObjectMapper()
        .registerKotlinModule() // Registers support for Kotlin features
        .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()) // Support for Java 8 time
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Optional: Use ISO-8601 format

    return Serdes.serdeFrom(
        { _, data ->
            objectMapper.writeValueAsBytes(data)
        },
        { _, bytes ->
            objectMapper.readValue(bytes, clazz)
        }
    )
}

class CustomSerde(
    statusSerde: Serde<DialogmeldingStatusDTO>,
    mottakSerde: Serde<DialogmeldingMottakDTO>
) : Serde<Any> {

    private val deserializer = CustomDeserializer(statusSerde, mottakSerde)
    private val serializer = CustomSerializer(statusSerde, mottakSerde)

    override fun serializer(): Serializer<Any> = serializer
    override fun deserializer(): Deserializer<Any> = deserializer
}

class CustomDeserializer(
    private val statusSerde: Serde<DialogmeldingStatusDTO>,
    private val mottakSerde: Serde<DialogmeldingMottakDTO>
) : Deserializer<Any> {
    override fun deserialize(topic: String?, data: ByteArray?): Any {
        val json = String(data ?: throw IllegalArgumentException("Null message"))
        return try {
            // Attempt to deserialize as DialogmeldingStatusDTO
            statusSerde.deserializer().deserialize(topic, data)
        } catch (e: Exception) {
            // If that fails, attempt to deserialize as DialogmeldingMottakDTO
            mottakSerde.deserializer().deserialize(topic, data)
        }
    }
}

class CustomSerializer(
    private val statusSerde: Serde<DialogmeldingStatusDTO>,
    private val mottakSerde: Serde<DialogmeldingMottakDTO>
) : Serializer<Any> {

    override fun serialize(topic: String?, data: Any?): ByteArray {
        return when (data) {
            is DialogmeldingStatusDTO -> statusSerde.serializer().serialize(topic, data)
            is DialogmeldingMottakDTO -> mottakSerde.serializer().serialize(topic, data)
            else -> throw IllegalArgumentException("Unknown type for serialization")
        }
    }
}