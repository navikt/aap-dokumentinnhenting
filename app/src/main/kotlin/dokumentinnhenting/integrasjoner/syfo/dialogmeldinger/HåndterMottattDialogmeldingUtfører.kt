package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.jobber.TaSakAvVentUtfører
import dokumentinnhenting.integrasjoner.dokarkiv.DokArkivClient
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class HåndterMottattDialogmeldingUtfører(private val dialogmeldingRepository: DialogmeldingRepository, val dokArkivClient: DokArkivClient, private val flytJobbRepository: FlytJobbRepository) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload = DefaultJsonMapper.fromJson<DialogmeldingMedSaksknyttning>(input.payload())

        val record = payload.dialogmeldingMottatt
        val sakOgBehandling = payload.sakOgBehandling

        if (eksistererBestillingPåPerson(record.personIdentPasient)) {
            dokArkivClient.knyttJournalpostTilAnnenSak(
                record.journalpostId,
                OpprettJournalpostRequest.Bruker(
                    record.personIdentPasient,
                    OpprettJournalpostRequest.Bruker.IdType.FNR
                ),
                record.personIdentPasient,
                "Kelvin" //TODO: riktig skrivemåte
            )
            val jobb = JobbInput(TaSakAvVentUtfører).medPayload(
                DefaultJsonMapper.toJson(DialogmeldingMedSaksknyttning(record, sakOgBehandling))
            )
            flytJobbRepository.leggTil(jobb)

        } else {
            val journalPostId = dokArkivClient.kopierJournalpostForDialogMelding(
                journalPostId = record.journalpostId,
                eksternReferanseId = sakOgBehandling.saksnummer
            )

            val jobb = JobbInput(EndreTemaUtfører).medPayload(
                journalPostId
            )
            flytJobbRepository.leggTil(jobb)
        }
    }

    private fun eksistererBestillingPåPerson(personId: String): Boolean {
        return dialogmeldingRepository.hentSisteBestillingByPIDYngreEnn2mMnd(personId) != null
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return HåndterMottattDialogmeldingUtfører(
                DialogmeldingRepository(
                    connection
                ),
                DokArkivClient(ClientCredentialsTokenProvider),
                FlytJobbRepository(connection)
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
