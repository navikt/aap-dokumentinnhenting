package dokumentinnhenting.integrasjoner.syfo.status

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

const val SYFO_STATUS_DIALOGMELDING_TOPIC = "teamsykefravr.behandler-dialogmelding-status"

class DialogmeldingStatusStream(
    private val datasource: DataSource,
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource),
) {
    private val log = LoggerFactory.getLogger(DialogmeldingStatusStream::class.java)
    val topology: Topology

    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingStatusDTOSerde())
        )
            .filter { _, record -> hentSakListe().contains(record.bestillingUuid) }
            .foreach { _, record -> oppdaterStatus(record) }

        topology = streamBuilder.build()
    }

    //TODO: Hensiktsmessig å batche?
    private fun oppdaterStatus(record: DialogmeldingStatusDTO) {
        transactionProvider.inTransaction {
            log.info("Oppdaterer status på ${record.bestillingUuid} med status ${record.status}")
            dialogmeldingRepository.oppdaterDialogmeldingStatus(record)
            if (record.status == MeldingStatusType.AVVIST) {
                taSakAvVent(record)
            }
        }
    }

    private fun hentSakListe(): List<String> {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            repository.hentalleDialogIder()
        }
    }

    private fun taSakAvVent(record: DialogmeldingStatusDTO) {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            val sak = repository.hentByDialogId(UUID.fromString(record.bestillingUuid))

            val behandlingsflytClient = BehandlingsflytClient()
            behandlingsflytClient.taSakAvVent(sak.behandlingsReferanse, BehandlingsflytClient.TaAvVentRequest(0L, "Avvist hos SYFO med begrunnelse: ${record.tekst}"))
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