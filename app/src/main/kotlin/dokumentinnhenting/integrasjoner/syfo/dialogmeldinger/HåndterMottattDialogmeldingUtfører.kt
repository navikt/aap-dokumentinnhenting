package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.azure.SystemTokenProvider
import dokumentinnhenting.integrasjoner.behandlingsflyt.jobber.TaSakAvVentUtfører
import dokumentinnhenting.integrasjoner.dokarkiv.DokarkivGateway
import dokumentinnhenting.integrasjoner.dokarkiv.KnyttTilAnnenSakRequest
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class dummyJobbUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {
        println("dummyJobbUtfører")
    }
}

class HåndterMottattDialogmeldingUtfører(
    private val dokArkivGateway: DokarkivGateway,
    private val flytJobbRepository: FlytJobbRepository,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload = DefaultJsonMapper.fromJson<DialogmeldingMedSakstilknytning>(input.payload())


        val record = payload.dialogmeldingMottatt
        val sakOgBehandling = payload.sakOgBehandling
        if (record.journalpostId == "0" && Miljø.er() == MiljøKode.DEV) {
            return dummyJobbUtfører().utfør(input)
        }

        runBlocking {
            dokArkivGateway.knyttJournalpostTilAnnenSak(
                record.journalpostId,
                KnyttTilAnnenSakRequest(
                    OpprettJournalpostRequest.Bruker(
                        record.personIdentPasient,
                        OpprettJournalpostRequest.Bruker.IdType.FNR
                    ),
                    payload.sakOgBehandling.saksnummer,
                    "KELVIN" //TODO: riktig skrivemåte
                )
            )
        }

        val jobb = JobbInput(TaSakAvVentUtfører).medPayload(
            DefaultJsonMapper.toJson(DialogmeldingMedSakstilknytning(record, sakOgBehandling))
        )
        flytJobbRepository.leggTil(jobb)
    }


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return HåndterMottattDialogmeldingUtfører(
                DokarkivGateway(SystemTokenProvider),
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
