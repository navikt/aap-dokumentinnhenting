package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.brev.BrevClient
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvvistLegeerklæringId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class OppdaterLegeerklæringStatusUtfører (
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val jobbRepository: FlytJobbRepository
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val record = DefaultJsonMapper.fromJson<DialogmeldingStatusDTO>(input.payload())
        val bestillingId = dialogmeldingRepository.låsBestilling(UUID.fromString(record.bestillingUuid))
        val eksisterendeRecord = dialogmeldingRepository.hentByDialogId(bestillingId)
        dialogmeldingRepository.oppdaterDialogmeldingStatus(record)

        if (eksisterendeRecord != null && eksisterendeRecord.status == record.status) {
            return
        }

        val avvistLegeerklæringId = UUID.randomUUID()

        if (record.status == MeldingStatusType.AVVIST) {
            val sak = requireNotNull(dialogmeldingRepository.hentByDialogId(bestillingId))
            val behandlingsflytClient = BehandlingsflytClient
            log.info("Avvist dialogmelding. Kaller behandlingsflyt. Sak: ${sak.saksnummer}")
            behandlingsflytClient.taSakAvVent(
                Innsending(
                    saksnummer = Saksnummer(sak.saksnummer),
                    referanse = InnsendingReferanse(AvvistLegeerklæringId(avvistLegeerklæringId)),
                    type = InnsendingType.LEGEERKLÆRING_AVVIST,
                    kanal = Kanal.DIGITAL,
                    // Bruker .now() i stedet for createdAt, siden dette er når *vi* mottok meldingen
                    mottattTidspunkt = LocalDateTime.now(),
                    melding = null
                )
            )
        }
        else if (record.status == MeldingStatusType.OK) {
            val sak = requireNotNull(dialogmeldingRepository.hentByDialogId(bestillingId))
            val brevClient = BrevClient()
            brevClient.ekspederBestilling(
                BrevClient.EkspederBestillingRequest(
                    requireNotNull(sak.journalpostId), (requireNotNull(sak.dokumentId))
                )
            )

            if (sak.dokumentasjonType.skalVarsleBruker()) {
                val jobb =
                    JobbInput(SendVarslingsbrevUtfører)
                        .medCallId()
                        .medPayload(DefaultJsonMapper.toJson(sak))
                jobbRepository.leggTil(jobb)
            }
        }
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OppdaterLegeerklæringStatusUtfører(
                DialogmeldingRepository(connection),
                FlytJobbRepository(connection)
            )
        }

        override fun type(): String {
            return "oppdaterStatusLegeerklæring"
        }

        override fun navn(): String {
            return "Oppdaterer status på bestilling av legeerklæring"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å gjennomføre statusoppdatering på legeerklæringer"
        }
    }
}