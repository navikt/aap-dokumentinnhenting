package dokumentinnhenting.util.metrics

import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

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

fun MeterRegistry.recordDialogmeldingStatusEndret(status: MeldingStatusType) {
    Counter.builder("dokumentinnhenting_dialogmelding_status_endret_total")
        .description("Antall behandlede statusendringer fra Syfo, uten gjentatte oppdateringer med samme status")
        .tag("status", status.name)
        .register(this)
        .increment()
}

enum class DialogmeldingFiltreringsutfall {
    SAMTALE, PARENT, BEHANDLINGSFLYT, INGEN_SAK, MANGLER_FORESPORSELSSVAR, UGYLDIG_JOURNALPOST
}

fun MeterRegistry.recordDialogmeldingFiltrert(utfall: DialogmeldingFiltreringsutfall) {
    Counter.builder("dokumentinnhenting_dialogmelding_filtrert_total")
        .description("Antall fullførte filtreringsforsøk for mottatte dialogmeldinger, fordelt på utfall")
        .tag("utfall", utfall.name.lowercase())
        .register(this)
        .increment()
}

enum class Journalforingsutfall {
    VELLYKKET, IKKE_FERDIGSTILT, TIMEOUT, FEIL
}

fun MeterRegistry.recordJournalforingsforsok(utfall: Journalforingsutfall) {
    Counter.builder("dokumentinnhenting_journalforingsforsok_total")
        .description("Antall journalføringsforsøk fordelt på utfall, inkludert nye forsøk")
        .tag("utfall", utfall.name.lowercase())
        .register(this)
        .increment()
}

fun MeterRegistry.recordAutomatiskPurringEndret(aktiv: Boolean) {
    Counter.builder("dokumentinnhenting_automatisk_purring_handlinger_total")
        .description("Antall fullførte handlinger for å avbryte eller gjenoppta automatisk purring")
        .tag("handling", if (aktiv) "gjenopptatt" else "avbrutt")
        .register(this)
        .increment()
}
