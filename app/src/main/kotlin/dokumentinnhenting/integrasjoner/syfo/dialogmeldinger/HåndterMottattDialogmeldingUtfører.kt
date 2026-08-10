package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.azure.SystemTokenProvider
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.behandlingsflyt.jobber.TaSakAvVentUtfører
import dokumentinnhenting.integrasjoner.dokarkiv.DokarkivGateway
import dokumentinnhenting.integrasjoner.dokarkiv.KnyttTilAnnenSakRequest
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest
import dokumentinnhenting.prosessering.medDialogmeldingUuid
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører


class HåndterMottattDialogmeldingUtfører(
    private val dokArkivGateway: DokarkivGateway,
    private val flytJobbRepository: FlytJobbRepository,
    private val mottattDialogmeldingRepository: MottattDialogmeldingRepository,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload = DefaultJsonMapper.fromJson<FiltrertDialogmeldingMedSakstilknytning>(input.payload())

        val dialogmelding = payload.dialogmeldingMottatt
        val saksnummer = payload.sakOgBehandling.saksnummer

        if (payload.skalLagreMottattDialogmelding) {
            mottattDialogmeldingRepository.lagre(dialogmelding, saksnummer)
        }

        runBlocking {
            dokArkivGateway.knyttJournalpostTilAnnenSak(
                dialogmelding.journalpostId,
                KnyttTilAnnenSakRequest(
                    OpprettJournalpostRequest.Bruker(
                        dialogmelding.personIdentPasient,
                        OpprettJournalpostRequest.Bruker.IdType.FNR
                    ),
                    saksnummer,
                    "KELVIN"
                )
            )
        }

        val jobb = JobbInput(TaSakAvVentUtfører).medPayload(
            DefaultJsonMapper.toJson(DialogmeldingMedSakstilknytning(dialogmelding, payload.sakOgBehandling))
        ).medDialogmeldingUuid(dialogmelding.msgId)

        flytJobbRepository.leggTil(jobb)
    }


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return HåndterMottattDialogmeldingUtfører(
                DokarkivGateway(SystemTokenProvider),
                FlytJobbRepository(connection),
                MottattDialogmeldingRepository(connection),
            )
        }

        override fun type(): String {
            return "dialogmelding.handler"
        }

        override fun navn(): String {
            return "Håndter mottatte dialogmeldinger"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å håndtere relevante dialogmeldinger"
        }
    }
}
