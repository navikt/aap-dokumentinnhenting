package integrasjonportal.integrasjoner.syfo.status

import integrasjonportal.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

const val SYFO_STATUS_DIALOGMELDING_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
/*
private const val TEMA = "AAP" // what here
private const val MOTTATT = "MOTTATT" // what here
private const val EESSI = "EESSI" // what here
*/

// TODO:
//  Feilhåndtering (Må bruke jobbmotor), 1 steg for write to stream, 1 steg for write to db
//  Må få lagd repo db, og kunne hente disse ut til filtrering kontinuerlig
//  Må avklare om syfo kan pushe tagsene over på det mottatte dokumentet, slik at postmottak faktisk kan hente det ut

class DialogmeldingStatusStream(
    datasource: DataSource,
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource)
) {
    private val log = LoggerFactory.getLogger(DialogmeldingStatusStream::class.java)
    val topology: Topology

    init {
        val streamBuilder = StreamsBuilder()
        streamBuilder.stream(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingStatusDTOSerde())
        )
            .filter { _, record -> record.bestillingUuid == "" } // Eller blir det dialogmeldingUUid? Hva er forskjell?
            .foreach { _, record -> oppdaterStatus(record) }

        topology = streamBuilder.build()
    }

    // TODO: Er det hensiktmessig å batche her eller ei?
    private fun oppdaterStatus(record: DialogmeldingStatusDTO) {
        transactionProvider.inTransaction {
            dialogmeldingRepository.oppdaterDialogmeldingStatus(record)
        }
    }
}

class TransactionContext(
    val dialogmeldingRepository: DialogmeldingRepository
)

class TransactionProvider(
    val datasource: DataSource
) {
    //Todo: jobb-implementasjon i block her?
    fun inTransaction(block: TransactionContext.() -> Unit) {
        datasource.transaction {
            TransactionContext(DialogmeldingRepository(it))
        }
    }
}