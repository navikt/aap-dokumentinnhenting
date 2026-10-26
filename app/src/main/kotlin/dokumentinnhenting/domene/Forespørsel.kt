package dokumentinnhenting.domene

import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class Forespørsel(
    val melding: UtgåendeDialogmelding,
    val dokumentasjonstype: DokumentasjonType,
    val automatiskPåminnelseAktiv: Boolean,
    val påminnelser: List<Påminnelse>,
    val påminnelsesplan: Påminnelsesplan? = null,
) {
    init {
        require(påminnelser.all { it.forespørselId == melding.id }) {
            "Alle påminnelser må referere til forespørselen"
        }
        require(påminnelsesplan == null || påminnelsesplan.forespørselId == melding.id) {
            "Påminnelsesplanen må referere til forespørselen"
        }
        require(påminnelsesplan == null || påminnelsesplan.behandlingsreferanse == melding.behandlingsreferanse) {
            "Påminnelsesplanen må tilhøre samme behandling som forespørselen"
        }
    }

    fun harPåminnelse(): Boolean = påminnelser.isNotEmpty()

    fun sistePåminnelse(): Påminnelse? =
        påminnelser.maxByOrNull { it.melding.opprettetTidspunkt }
}

data class Påminnelse(
    val melding: UtgåendeDialogmelding,
    val forespørselId: UUID,
)

data class UtgåendeDialogmelding(
    val id: UUID,
    val behandlingsreferanse: BehandlingReferanse,
    val saksnummer: Saksnummer,
    val person: Person,
    val behandler: Behandler,
    val bestillerNavIdent: String,
    val tekst: String,
    val opprettetTidspunkt: LocalDateTime,
    val journalreferanse: Journalreferanse?,
    val gjeldendeStatus: MeldingStatusType?,
    val statusTekst: String?,
    val samtaleRef: UUID,
    val flytStatus: ProsesseringSyfoStatus?,
    val statushistorikk: List<Statushendelse>,
)

data class Statushendelse(
    val kildehendelseId: String,
    val status: MeldingStatusType,
    val hendelseTidspunkt: Instant,
    val mottattTidspunkt: Instant,
    val tekst: String?,
)

data class Person(
    val ident: String,
    val navn: String,
)

data class Behandler(
    val referanse: String,
    val hprNummer: String,
    val navn: String,
)

data class Journalreferanse(
    val journalpostId: JournalpostId?,
    val dokumentId: String?,
)
