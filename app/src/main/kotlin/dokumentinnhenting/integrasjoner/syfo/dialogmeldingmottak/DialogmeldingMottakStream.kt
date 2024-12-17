package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.dokarkiv.DokArkivClient
import dokumentinnhenting.integrasjoner.dokarkiv.OpprettJournalpostRequest
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource

const val SYFO_DIALOGMELDING_MOTTAK_TOPIC = "teamsykefravr.dialogmelding"

class DialogmeldingMottakStream(
    private val datasource: DataSource,
    val behandlingsflytClient: BehandlingsflytClient,
    val dokArkivClient: DokArkivClient
) {
    private val log = LoggerFactory.getLogger(DialogmeldingMottakStream::class.java)
    val topology: Topology


    init {
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(
            SYFO_DIALOGMELDING_MOTTAK_TOPIC,
            Consumed.with(Serdes.String(), dialogmeldingMottakDTOSerde())
        )
            .mapValues { _, record -> record to behandlingsflytClient.åpenSakEksisterer() }
            .filter({ _, (record, saksnummer) -> saksnummer != null })
            .split().branch(
                {_, (record, saksnummer) -> eksistererBestillingPåPerson(saksnummer!!.personIdent)},//bstilling eksisterer,
                Branched.withConsumer({chain -> chain.foreach{_, (record, saksnummer) ->
                    dokArkivClient.knyttJournalpostTilAnnenSak(
                        record.journalpostId,
                        OpprettJournalpostRequest.Bruker(
                            record.personIdentPasient,
                            OpprettJournalpostRequest.Bruker.IdType.FNR
                        ),
                        record.personIdentPasient,
                        "Kelvin" //TODO: riktig skrivemåte
                    )
                }}
                )
            ).defaultBranch(
                Branched.withConsumer({chain ->
                    chain.foreach{_, (record, saksnummer) ->
                        val journalPostId = dokArkivClient.kopierJournalpostForDialogMelding(
                            journalPostId = record.journalpostId,
                            eksternReferanseId = saksnummer?.sakId!!
                        )
                        dokArkivClient.endreTemaTilAAP(journalPostId)
                }})
            )

        topology = streamBuilder.build()

    }

    private fun eksistererBestillingPåPerson(personId:String): Boolean {
        return datasource.transaction { connection ->
            val repository = DialogmeldingRepository(connection)
            repository.hentSisteBestillingByPIDYngreEnn2mMnd(personId)!=null
        }
    }

}

