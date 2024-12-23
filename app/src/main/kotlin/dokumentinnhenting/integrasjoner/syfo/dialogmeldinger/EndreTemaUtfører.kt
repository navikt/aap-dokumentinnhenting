package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.dokarkiv.DokArkivClient
import dokumentinnhenting.integrasjoner.dokarkiv.kopierJournalpostResponse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class EndreTemaUtfører(val dokArkivClient: DokArkivClient):JobbUtfører {
    override fun utfør(input: JobbInput) {
        val journalpostId = DefaultJsonMapper.fromJson<kopierJournalpostResponse>(input.payload()).kopierJournalpostId

        dokArkivClient.endreTemaTilAAP(journalpostId)
    }


    companion object : Jobb {
        override fun beskrivelse(): String {
            return "Ansvarlig for å endre tema etter kopi av en journalpost"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return EndreTemaUtfører(DokArkivClient())
        }

        override fun navn(): String {
            return "endreTema"
        }

        override fun type(): String {
            return "endreTema"
        }

    }
}