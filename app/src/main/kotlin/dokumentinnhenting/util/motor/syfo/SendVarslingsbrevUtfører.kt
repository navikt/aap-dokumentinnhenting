package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.behandlingsflyt.VarselOmBrevbestillingDto
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class SendVarslingsbrevUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingsflytClient = BehandlingsflytClient()

        val record = DefaultJsonMapper.fromJson<DialogmeldingFullRecord>(input.payload())

        behandlingsflytClient.sendVarslingsbrev(
            VarselOmBrevbestillingDto(
                BehandlingReferanse(record.behandlingsReferanse),
                Vedlegg(
                    record.journalpostId!!,
                    record.dokumentId!!
                )
            )
        )
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendVarslingsbrevUtfører()
        }

        override fun type(): String {
            return "sendVarslingsbrev"
        }

        override fun navn(): String {
            return "Bestiller et varslingsbrev til aktuelle bruker i brev via behandlingsflyt"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å gjennomføre bestilling av varslingsbrev til bruker"
        }
    }
}