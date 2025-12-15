package dokumentinnhenting.integrasjoner.behandlingsflyt.jobber

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.DialogmeldingMedSaksknyttning
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal

class TaSakAvVentUtfører(private val behandlingsflytGateway: BehandlingsflytGateway) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload = DefaultJsonMapper.fromJson<DialogmeldingMedSaksknyttning>(input.payload())

        val record = payload.dialogmeldingMottatt
        val sakOgBehandling = payload.sakOgBehandling

        behandlingsflytGateway.taSakAvVent(
            Innsending(
                Saksnummer(sakOgBehandling.saksnummer),
                referanse = InnsendingReferanse(
                    JournalpostId(record.journalpostId),
                ),
                type = InnsendingType.DIALOGMELDING,
                kanal = Kanal.DIGITAL,
                mottattTidspunkt = record.mottattTidspunkt,
                melding = null
            )
        )
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return TaSakAvVentUtfører(
                BehandlingsflytGateway
            )
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å ta saker av vent i behandlingsflyt"
        }

        override fun navn(): String {
            return "Ta saker av vent i behandlingsflyt"
        }

        override fun type(): String {
            return "taSakAvVent.handler"
        }

    }
}