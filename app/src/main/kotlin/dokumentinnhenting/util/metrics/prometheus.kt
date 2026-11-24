package dokumentinnhenting.util.metrics

import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun MeterRegistry.bestillingCounter(topic: String): Counter =
    this.counter("bestilling", listOf(Tag.of("topic", topic)))

fun MeterRegistry.recordSafBrukerJournalposterCount(count: Int) {
    this.counter("dokumentinnhenting_antall_saf_bruker_journalposter_returnert").increment(count.toDouble())
}

fun MeterRegistry.recordPurringBestilt(automatisk: Boolean) {
    Counter.builder("dokumentinnhenting_purring_bestilt_total")
        .description("Antall purringer lagt i kø for behandling")
        .tag("type", if (automatisk) "automatisk" else "manuell")
        .register(this)
        .increment()
}

fun MeterRegistry.recordDialogmeldingSendt(dokumentasjonType: DokumentasjonType) {
    Counter.builder("dokumentinnhenting_dialogmelding_sendt_total")
        .description("Antall dialogmeldinger sendt til Kafka")
        .tag("dokumentasjonstype", dokumentasjonType.name)
        .register(this)
        .increment()
}
