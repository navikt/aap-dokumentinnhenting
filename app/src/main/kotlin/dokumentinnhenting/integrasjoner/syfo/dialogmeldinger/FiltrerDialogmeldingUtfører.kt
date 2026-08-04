package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.repositories.DialogmeldingRepository
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class FiltrerDialogmeldingUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val dialogmeldingRepository: DialogmeldingRepository,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val payload: DialogmeldingMottakDTO =
            DefaultJsonMapper.fromJson<DialogmeldingMottakDTO>(input.payload())

        if (Miljø.erDev() && payload.journalpostId == "0") {
            // Skal kun skje i testmiljøet. Vil være "0" i tilfeller hvor Syfo ikke klarer å opprette journalpost.
            log.warn("Håndterer ikke dialogmelding fordi journalpostId er 0.")
            return
        }

        val sendtDialogmelding =
            payload.conversationRef?.toUUIDOrNull()
                ?.let {
                    dialogmeldingRepository.hentForSamtale(
                        samtaleRef = it,
                        personIdent = payload.personIdentPasient
                    )
                }
                ?.maxByOrNull { it.opprettet }
                ?.also { log.info("Fant kobling fra mottatt til sendt dialogmelding basert på conversationRef.") }
                ?: payload.parentRef?.toUUIDOrNull()
                    ?.let {
                        dialogmeldingRepository.hentForParent(
                            parentRef = it,
                            personIdent = payload.personIdentPasient
                        )
                    }
                    ?.also { log.info("Fant kobling fra mottatt til sendt dialogmelding basert på parentRef.") }

        if (sendtDialogmelding != null) {
            log.info("Er mottatt dialogmelding svar på forespørsel? ${payload.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null}")
            opprettJobb(payload, sendtDialogmelding.saksnummer, skalLagreMottatDialogmelding = true)
        } else if (payload.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null) {
            log.info("Fant ikke kobling fra mottatt til sendt dialogmelding. Henter saksinfo fra behandlingsflyt for dialogmelding med journalpostId ${payload.journalpostId}")
            val saksInfo = BehandlingsflytGateway.finnÅpenSakForIdentPåDato(
                payload.personIdentPasient,
                payload.mottattTidspunkt.toLocalDate()
            )

            if (saksInfo == null) {
                log.info("Fant ikke åpen sak for personident på dialogmelding med journalpostId ${payload.journalpostId}")
                return
            } else {
                log.info("Fant åpen sak for dialogmelding med journalpostId ${payload.journalpostId}")
                opprettJobb(payload, saksInfo.saksnummer, skalLagreMottatDialogmelding = false)
            }
        }
    }

    private fun opprettJobb(
        mottattDialogmelding: DialogmeldingMottakDTO,
        saksnummer: String,
        skalLagreMottatDialogmelding: Boolean,
    ) {
        flytJobbRepository.leggTil(
            JobbInput(HåndterMottattDialogmeldingUtfører).medPayload(
                DefaultJsonMapper.toJson(
                    FiltrertDialogmeldingMedSakstilknytning(
                        skalLagreMottattDialogmelding = skalLagreMottatDialogmelding,
                        dialogmeldingMottatt = mottattDialogmelding,
                        sakOgBehandling = BehandlingsflytGateway.SakOgBehandling(saksnummer),
                    )
                )
            )
        )
    }

    fun String.toUUIDOrNull(): UUID? {
        return runCatching {
            UUID.fromString(this)
        }.getOrNull()
    }

    companion object : Jobb {
        override fun beskrivelse(): String {
            return "Ansvarlig for å filtrere dialogmeldinger som vi har sak på"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FiltrerDialogmeldingUtfører(
                flytJobbRepository = FlytJobbRepository(connection),
                dialogmeldingRepository = DialogmeldingRepository(connection),
            )
        }

        override fun navn(): String {
            return "Filtererer mottatt dialogmelding"
        }

        override fun type(): String {
            return "dialogmelding.filter"
        }
    }
}
