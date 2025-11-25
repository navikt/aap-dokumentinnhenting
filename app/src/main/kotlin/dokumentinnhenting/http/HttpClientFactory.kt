package dokumentinnhenting.http

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.time.Duration

object HttpClientFactory {
    fun create(
        connectTimeoutMs: Long = Duration.ofSeconds(10).toMillis(),
        requestTimeoutMs: Long = Duration.ofSeconds(30).toMillis(),
        maxRetries: Int = 2
    ): HttpClient = HttpClient(CIO) {
        expectSuccess = false

        install(HttpTimeout) {
            connectTimeoutMillis = connectTimeoutMs
            requestTimeoutMillis = requestTimeoutMs
            socketTimeoutMillis = requestTimeoutMs
        }

        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }

        if (maxRetries > 0) {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = maxRetries)
                exponentialDelay()
            }
        }
    }
}
