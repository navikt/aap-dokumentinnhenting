package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class SendVarslingsbrevUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingsflytClient = BehandlingsflytClient()
        behandlingsflytClient.sendVarslingsbrev()
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