package tilgang.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun PrometheusMeterRegistry.cacheHit(service: String): Counter = this.counter(
    "cache_hit",
    listOf(Tag.of("service", service))
)

fun PrometheusMeterRegistry.cacheMiss(service: String): Counter = this.counter(
    "cache_miss",
    listOf(Tag.of("service", service))
)

fun PrometheusMeterRegistry.httpCallCounter(path: String): Counter = this.counter(
    "http_call",
    listOf(Tag.of("path", path))
)
