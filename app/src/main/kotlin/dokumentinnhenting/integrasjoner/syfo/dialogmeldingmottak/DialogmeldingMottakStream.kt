package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource

const val SYFO_DIALOGMELDING_MOTTAK_TOPIC = "teamsykefravr.dialogmelding"

class DialogmeldingMottakStream(private val datasource: DataSource) {
    private val log = LoggerFactory.getLogger(DialogmeldingMottakStream::class.java)
    val topology: Topology


    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_DIALOGMELDING_MOTTAK_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingMottakDTOSerde())
        )
            .mapValues { _, record -> record to hentBestillingsListe(record.personIdentPasient) }
            .filter({ _, (record, saksnummer) -> saksnummer != null })
            .foreach { _, (record, saksnummer) -> return@foreach } //TODO: Denne må implementeres

        topology = streamBuilder.build()

    }

    private fun hentBestillingsListe(personId:String): String? {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            repository.hentSisteBestillingByPIDYngreEnn2mMnd(personId)
        }
    }

}

