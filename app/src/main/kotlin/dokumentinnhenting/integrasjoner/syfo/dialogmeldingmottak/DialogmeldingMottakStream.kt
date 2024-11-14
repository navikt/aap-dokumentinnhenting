package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import dokumentinnhenting.integrasjoner.syfo.status.TransactionProvider
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

const val SYFO_DIALOGMELDING_MOTTAK_TOPIC = "teamsykefravr.dialogmelding"

class DialogmeldingMottakStream(private val datasource: DataSource) {
    private val log = LoggerFactory.getLogger(DialogmeldingMottakStream::class.java)
    val topology: Topology

    @Volatile
    private var bestillinger: MutableList<DialogmeldingRepository.DialogMeldingBestillingPersoner> =
        hentBestillingsListe().toMutableList()


    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_DIALOGMELDING_MOTTAK_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingMottakDTOSerde())
        )
            .filter { _, record -> bestillinger.map { it.personId }.contains(record.personIdentPasient) }
            .foreach { _, record -> TODO() }

        topology = streamBuilder.build()

        scheduleDialogMeldingerRefresh()
    }

    private fun scheduleDialogMeldingerRefresh() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            {
                log.info("Henter bestillinger...")
                bestillinger = hentBestillingsListe().toMutableList()
                log.info("Fant bestillinger: ${bestillinger.count()}")
            },
            0, 1, TimeUnit.MINUTES
        )

    }

    private fun hentBestillingsListe(): List<DialogmeldingRepository.DialogMeldingBestillingPersoner> {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            repository.hentAlleDialogmeldingerYngreEnn2mMnd()
        }
    }

}