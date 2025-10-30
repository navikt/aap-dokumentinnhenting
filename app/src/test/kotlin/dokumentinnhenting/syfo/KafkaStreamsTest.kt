package dokumentinnhenting.syfo

import dokumentinnhenting.integrasjoner.syfo.SYFO_STATUS_DIALOGMELDING_TOPIC
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.createDialogmeldingStreamTopology
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.HenvendelseFraLegeHenvendelse
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.kafka.CustomSerde
import dokumentinnhenting.util.kafka.createGenericSerde
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals

class KafkaStreamsTest {
    private lateinit var inputTopic: TestInputTopic<String, Any>
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
        val topology = createDialogmeldingStreamTopology(dataSource)

        val props = Properties().apply {
            put("application.id", "test-app")
            put("bootstrap.servers", "dummy:1234")
        }

        testDriver = TopologyTestDriver(topology, props)

        val customSerde = CustomSerde(
            createGenericSerde(DialogmeldingStatusDTO::class.java),
            createGenericSerde(DialogmeldingMottakDTO::class.java)
        )

        inputTopic = testDriver.createInputTopic(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Serdes.String().serializer(),
            customSerde.serializer()
        )
    }

    @AfterEach
    fun teardown() {
        testDriver.close()
        dataSource.close()
    }

    @Test
    fun `process dialogmelding status`() {
        val uuid = UUID.randomUUID()
        val bestillingUuid = uuid.toString()

        val saksnummer = "saksnummer"
        val existingRecord = DialogmeldingRecord(
            bestillerNavIdent = "bestillerNavIdent",
            dialogmeldingUuid = uuid,
            behandlerRef = "behandlerRef",
            behandlerHprNr = "hpr12344321",
            personIdent = "personIdent",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dokumentasjonType = DokumentasjonType.L8,
            behandlerNavn = "behandlernavn",
            fritekst = "fritekst",
            behandlingsReferanse = UUID.randomUUID()
        )

        setupRepositoryDataStatus(dataSource, existingRecord)

        val incomingRecord = DialogmeldingStatusDTO(
            bestillingUuid = bestillingUuid,
            status = MeldingStatusType.OK,
            tekst = "Teststatus",
            createdAt = OffsetDateTime.now(),
            uuid = uuid.toString()
        )

        inputTopic.pipeInput("key", incomingRecord)

        val record = hentRepositoryDataStatus(dataSource, saksnummer)
        assertEquals(uuid, record[0].dialogmeldingUuid)
    }

    @Test
    fun `process dialogmelding mottak`() {
        val uuid = UUID.randomUUID()

        val saksnummer = "saksnummer"
        val existingRecord = DialogmeldingRecord(
            bestillerNavIdent = "bestillerNavIdent",
            dialogmeldingUuid = uuid,
            behandlerRef = "behandlerRef",
            behandlerHprNr = "hprIdent",
            personIdent = "personIdent",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dokumentasjonType = DokumentasjonType.L8,
            behandlerNavn = "behandlernavn",
            fritekst = "fritekst",
            behandlingsReferanse = UUID.randomUUID()
        )
        setupRepositoryDataMottak(dataSource, existingRecord)

        val incomingRecord = DialogmeldingMottakDTO(
            "msgId",
            "msgType",
            "navLogId",
            LocalDateTime.now(),
            "conversationRef",
            "parentRef",
            "personIdentPasient",
            "pasientAktoerId",
            "personIdentBehandler",
            "behandlerAktoerId",
            "legekontorOrgNr",
            "legekontorHerId",
            "legekontorReshId",
            "legekontorOrgName",
            "legehpr",
            Dialogmelding(
                "id",
                null,
                null,
                HenvendelseFraLegeHenvendelse(
                    TemaKode("kodeverkOID", "dn", "v", "kat", "kod", "tittel"),
                    "tekstNotatInnhold",
                    "dokIdNotat",
                    null,
                    null
                ),
                "navnHelsepersonell",
                LocalDateTime.now()
            ),
            1,
            "journalpostId",
            "fellesformatXML"
        )

        inputTopic.pipeInput("key", incomingRecord)
        val oppdatertHendelse = hentRepositoryDataMottak(dataSource, saksnummer)
        Assertions.assertEquals(uuid, oppdatertHendelse[0].dialogmeldingUuid)
    }

    private fun setupRepositoryDataStatus(dataSource: DataSource, record: DialogmeldingRecord) {
        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.opprettDialogmelding(record)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(
                record.dialogmeldingUuid,
                "1234",
                "1234"
            )
        }
    }

    private fun hentRepositoryDataStatus(
        dataSource: DataSource,
        saksnummer: String
    ): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
        }
    }

    private fun setupRepositoryDataMottak(dataSource: DataSource, record: DialogmeldingRecord) {
        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.opprettDialogmelding(record)
        }
    }

    private fun hentRepositoryDataMottak(
        dataSource: DataSource,
        saksnummer: String
    ): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
        }
    }
}