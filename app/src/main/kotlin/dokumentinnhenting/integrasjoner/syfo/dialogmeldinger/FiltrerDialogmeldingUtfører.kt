package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class FiltrerDialogmeldingUtfører(private val flytJobbRepository: FlytJobbRepository) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload: DialogmeldingMottakDTO = DefaultJsonMapper.fromJson<DialogmeldingMottakDTO>(input.payload())
        val saksInfo = BehandlingsflytClient.finnSakForIdentPåDato(
            payload.personIdentPasient,
            payload.mottattTidspunkt.toLocalDate()
        )

        if(saksInfo?.sakOgBehandlingDTO != null && payload.journalpostId!="0"){
            flytJobbRepository.leggTil(
                JobbInput(HåndterMottattDialogmeldingUtfører).medPayload(
                    DefaultJsonMapper.toJson(DialogmeldingMedSaksknyttning(payload, saksInfo.sakOgBehandlingDTO))
                )
            )
        }
    }

    companion object : Jobb {
        override fun beskrivelse(): String {
            return "Ansvarlig for å filtrere dialogmeldinger som vi har sak på"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FiltrerDialogmeldingUtfører(
                FlytJobbRepository(connection)
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