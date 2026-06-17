package dokumentinnhenting.syfo

import dokumentinnhenting.WithFakes
import dokumentinnhenting.api.tilDto
import dokumentinnhenting.integrasjoner.syfo.SYFO_DIALOGMELDING_MOTTAK_TOPIC
import dokumentinnhenting.integrasjoner.syfo.SYFO_STATUS_DIALOGMELDING_TOPIC
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.createDialogmeldingStreamTopology
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.HenvendelseFraLegeHenvendelse
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDto
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.kafka.createGenericSerde
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Properties
import java.util.UUID
import javax.sql.DataSource
import kotlin.random.Random
import kotlin.test.assertContains
import kotlin.test.assertEquals
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaStreamsTest {
    private lateinit var statusInputTopic: TestInputTopic<String, DialogmeldingStatusDto>
    private lateinit var mottakInputTopic: TestInputTopic<String, DialogmeldingMottakDTO>
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
        val topology = createDialogmeldingStreamTopology(dataSource)

        val props = Properties().apply {
            put("application.id", "test-app")
            put("bootstrap.servers", "dummy:1234")
        }

        testDriver = TopologyTestDriver(topology, props)

        statusInputTopic = testDriver.createInputTopic(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Serdes.String().serializer(),
            createGenericSerde(DialogmeldingStatusDto::class.java).serializer()
        )

        mottakInputTopic = testDriver.createInputTopic(
            SYFO_DIALOGMELDING_MOTTAK_TOPIC,
            Serdes.String().serializer(),
            createGenericSerde(DialogmeldingMottakDTO::class.java).serializer()
        )
    }

    @AfterAll
    fun teardown() {
        testDriver.close()
        dataSource.close()
    }

    @Test
    fun `topology konsumerer begge topics`() {
        val description = createDialogmeldingStreamTopology(dataSource).describe().toString()

        assertContains(description, SYFO_STATUS_DIALOGMELDING_TOPIC)
        assertContains(description, SYFO_DIALOGMELDING_MOTTAK_TOPIC)
    }

    @Test
    fun `process dialogmelding status`() {
        val uuid = UUID.randomUUID()
        val bestillingUuid = uuid.toString()

        val saksnummer = Random.nextLong().toString()
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
            behandlingsReferanse = UUID.randomUUID(),
            samtaleRef = UUID.randomUUID(),
        )

        setupRepositoryDataStatus(dataSource, existingRecord)

        val incomingRecord = DialogmeldingStatusDto(
            bestillingUuid = bestillingUuid,
            status = MeldingStatusType.OK,
            tekst = "Teststatus",
            createdAt = OffsetDateTime.now(),
            uuid = uuid.toString()
        )

        statusInputTopic.pipeInput("key", incomingRecord)

        val record = hentRepositoryDataStatus(dataSource, saksnummer)
        assertEquals(uuid, record[0].dialogmeldingUuid)
    }

    @Test
    fun `process dialogmelding mottak`() {
        val uuid = UUID.randomUUID()

        val saksnummer = Random.nextLong().toString()
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
            behandlingsReferanse = UUID.randomUUID(),
            samtaleRef = UUID.randomUUID(),
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

        mottakInputTopic.pipeInput("key", incomingRecord)
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
    ): List<DialogmeldingStatusTilBehandslingsflytDto> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
                .map(DialogmeldingFullRecord::tilDto)
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
        saksnummer: String,
    ): List<DialogmeldingStatusTilBehandslingsflytDto> {
        return dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
                .map(DialogmeldingFullRecord::tilDto)
        }
    }
}