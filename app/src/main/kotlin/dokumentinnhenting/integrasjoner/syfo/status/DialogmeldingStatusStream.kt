package dokumentinnhenting.integrasjoner.syfo.status

import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import java.util.*
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
            .filter { _, record -> bestillingEksisterer(record.bestillingUuid) }
            .foreach { _, record -> oppdaterStatus(record) }

        topology = streamBuilder.build()
    }

    private fun oppdaterStatus(record: DialogmeldingStatusDTO) {
        datasource.transaction{ connection ->
            val jobbRepository = FlytJobbRepository(connection)
            val dialogmeldingRepository = DialogmeldingRepository(connection)

            log.info("Oppdaterer status på ${record.bestillingUuid} med status ${record.status}")

            val lagretBestilling = requireNotNull(dialogmeldingRepository.hentByDialogId(UUID.fromString(record.bestillingUuid)))

            val jobb =
                JobbInput(OppdaterLegeerklæringStatusUtfører)
                    .medCallId()
                    .medPayload(DefaultJsonMapper.toJson(record))
                    .forSak(lagretBestilling.id)

            jobbRepository.leggTil(jobb)
        }
    }

    private fun bestillingEksisterer(bestillingUuid: String): Boolean {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            val record = repository.hentByDialogId(UUID.fromString(bestillingUuid))

            record?.dialogmeldingUuid.toString() == bestillingUuid
        }
    }
}