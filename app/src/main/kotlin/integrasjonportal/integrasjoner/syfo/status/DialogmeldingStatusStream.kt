package integrasjonportal.integrasjoner.syfo.status

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

private const val SYFO_STATUS_DIALOGMELDING_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
/*
private const val TEMA = "AAP" // what here
private const val MOTTATT = "MOTTATT" // what here
private const val EESSI = "EESSI" // what here
*/

// TODO:
//  Feilhåndtering (Må bruke jobbmotor), 1 steg for write to stream, 1 steg for write to db
//  Må få lagd repo db, og kunne hente disse ut til filtrering kontinuerlig
//  Må avklare om syfo kan pushe tagsene over på det mottatte dokumentet, slik at postmottak faktisk kan hente det ut

class DialogmeldingStatusStream{
    private val log = LoggerFactory.getLogger(DialogmeldingStatusStream::class.java)
    val topology: Topology


    init {
        val streamBuilder = StreamsBuilder()
        streamBuilder.stream(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingStatusDTOSerde())
        )
            .filter { _, record -> record.bestillingUuid == "" } // Eller blir det dialogmeldingUUid? Hva er forskjell?
            .foreach { _, record -> håndterStatus(record) }

        topology = streamBuilder.build()
    }

    // TODO: Er det hensiktmessig å batche her eller ei?
    private fun håndterStatus(record: DialogmeldingStatusDTO) {
        // Lagre status på hver entry? og så kan vi returnere på hvilke behandlingsId?
        // TODO: Implementer denne -> oppdaterDialogmeldingStatus(record)
        log.info("")
    }
}