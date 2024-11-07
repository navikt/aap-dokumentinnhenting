package dokumentinnhenting.integrasjoner.syfo.status

import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val SYFO_STATUS_DIALOGMELDING_TOPIC = "teamsykefravr.behandler-dialogmelding-status"

class DialogmeldingStatusStream(
    private val datasource: DataSource,
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource),
) {
    private val log = LoggerFactory.getLogger(DialogmeldingStatusStream::class.java)
    val topology: Topology

    @Volatile
    private var dialogMeldinger: MutableList<String> = hentSakListe().toMutableList()

    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingStatusDTOSerde())
        )
            .filter { _, record -> dialogMeldinger.contains(record.bestillingUuid) }
            .foreach { _, record -> oppdaterStatus(record) }

        topology = streamBuilder.build()

        scheduleDialogMeldingerRefresh()
    }


    private fun scheduleDialogMeldingerRefresh() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            {
                log.info("Henter saksliste...")
                dialogMeldinger = hentSakListe().toMutableList()
                log.info("Fant ${dialogMeldinger.count()}")
            },
            0, 1, TimeUnit.MINUTES
        )

    }

    //TODO: Hensiktsmessig å batche?
    private fun oppdaterStatus(record: DialogmeldingStatusDTO) {
        transactionProvider.inTransaction {
            dialogmeldingRepository.oppdaterDialogmeldingStatus(record)
        }
    }

    private fun hentSakListe(): List<String> {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            repository.hentalleSaksnumre()
        }
    }
}

class TransactionContext(
    val dialogmeldingRepository: DialogmeldingRepository
)

class TransactionProvider(
    val datasource: DataSource
) {
    fun inTransaction(block: TransactionContext.() -> Unit) {
        datasource.transaction {
            TransactionContext(DialogmeldingRepository(it)).block()
        }
    }
}