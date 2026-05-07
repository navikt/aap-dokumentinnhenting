package dokumentinnhenting.util.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
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
