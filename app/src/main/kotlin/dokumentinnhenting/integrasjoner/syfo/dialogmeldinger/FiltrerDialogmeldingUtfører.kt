package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.repositories.DialogmeldingRepository
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class FiltrerDialogmeldingUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val dialogmeldingRepository: DialogmeldingRepository,
) :
    JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val payload: DialogmeldingMottakDTO =
            DefaultJsonMapper.fromJson<DialogmeldingMottakDTO>(input.payload())

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
            if (payload.journalpostId == "0") {
                log.info("Håndterer ikke relevant dialogmelding fordi journalpostId er 0.")
                return
            }
            opprettJobb(payload, sendtDialogmelding.saksnummer)
        } else if (payload.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null) {
            log.info("Fant ikke kobling fra mottatt til sendt dialogmelding. Henter saksinfo fra behandlingsflyt for dialogmelding med journalpostId ${payload.journalpostId}")
            val saksInfo = BehandlingsflytGateway.finnÅpenSakForIdentPåDato(
                payload.personIdentPasient,
                payload.mottattTidspunkt.toLocalDate()
            ) ?: return

            if (payload.journalpostId == "0") {
                log.warn("Håndterer ikke relevant dialogmelding fordi journalpostId er 0.")
                return
            }

            opprettJobb(payload, saksInfo.saksnummer)
        }
    }

    private fun opprettJobb(mottattDialogmelding: DialogmeldingMottakDTO, saksnummer: String) {
        flytJobbRepository.leggTil(
            JobbInput(HåndterMottattDialogmeldingUtfører).medPayload(
                DefaultJsonMapper.toJson(
                    DialogmeldingMedSakstilknytning(
                        dialogmeldingMottatt = mottattDialogmelding,
                        sakOgBehandling = BehandlingsflytGateway.SakOgBehandling(saksnummer)
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
