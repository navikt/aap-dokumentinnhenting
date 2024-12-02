package dokumentinnhenting.integrasjoner.syfo.status

import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource

const val SYFO_STATUS_DIALOGMELDING_TOPIC = "teamsykefravr.behandler-dialogmelding-status"

class DialogmeldingStatusStream(
    private val datasource: DataSource
) {
    private val log = LoggerFactory.getLogger(DialogmeldingStatusStream::class.java)
    val topology: Topology

    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingStatusDTOSerde())
        )
            .filter { _, record -> hentSakListe().contains(record.bestillingUuid) } // Todo: Gjøre om disse?
            .foreach { _, record -> oppdaterStatus(record) }

        topology = streamBuilder.build()
    }

    private fun oppdaterStatus(record: DialogmeldingStatusDTO) {
        datasource.transaction{ connection ->
            val jobbRepository = FlytJobbRepository(connection)
            log.info("Oppdaterer status på ${record.bestillingUuid} med status ${record.status}")

            val jobb =
                JobbInput(OppdaterLegeerklæringStatusUtfører)
                    .medCallId()
                    .medPayload(DefaultJsonMapper.toJson(record))

            jobbRepository.leggTil(jobb)
        }
    }

    private fun hentSakListe(): List<String> {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            repository.hentalleDialogIder()
        }
    }
}