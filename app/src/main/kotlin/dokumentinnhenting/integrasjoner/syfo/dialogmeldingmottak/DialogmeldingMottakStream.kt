package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.DialogmeldingMedSaksknyttning
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.HåndterMottattDialogmeldingUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource

const val SYFO_DIALOGMELDING_MOTTAK_TOPIC = "teamsykefravr.dialogmelding"

class DialogmeldingMottakStream(
    private val datasource: DataSource,
    val behandlingsflytClient: BehandlingsflytClient
) {
    private val log = LoggerFactory.getLogger(DialogmeldingMottakStream::class.java)
    val topology: Topology


    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_DIALOGMELDING_MOTTAK_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingMottakDTOSerde())
        )
            .peek({ key, value -> log.info("Mottatt dialogmelding med msgId: $key") })
            .mapValues { _, record ->
                record to behandlingsflytClient.finnSakForIdentPåDato(
                    record.personIdentPasient,
                    record.mottattTidspunkt.toLocalDate()
                )
            }
            .filter({ _, (record, saksInfo) -> saksInfo?.sakOgBehandlingDTO != null })
            .foreach { _, (record, saksInfo) ->
                log.info("Oppretter jobb for dialogmelding med msgId: ${record.msgId}")
                opprettJobb(record, requireNotNull(saksInfo?.sakOgBehandlingDTO))
            }

        topology = streamBuilder.build()

    }

    private fun opprettJobb(dTO: DialogmeldingMottakDTO,
                            behandling: BehandlingsflytClient.SakOgBehandling) {
        datasource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)

            flytJobbRepository.leggTil(
                JobbInput(HåndterMottattDialogmeldingUtfører).medPayload(
                    DefaultJsonMapper.toJson(DialogmeldingMedSaksknyttning(dTO, behandling))
                )
            )
        }
    }

}

