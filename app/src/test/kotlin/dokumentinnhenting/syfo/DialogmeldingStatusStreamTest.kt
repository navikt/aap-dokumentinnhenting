package dokumentinnhenting.syfo

import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.*
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.ProsesseringsJobber
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.OffsetDateTime
import java.util.*

class DialogmeldingStatusStreamTest {
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, DialogmeldingStatusDTO>
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

    @BeforeEach
    fun setup() {
        InitTestDatabase.migrate()
        val dialogmeldingStatusStream = DialogmeldingStatusStream(dataSource)

        val topology = dialogmeldingStatusStream.topology

        val props = Properties().apply {
            put("app.yapp", "app")
            put("bootstrap.servers", "dummy:1234")
        }

        testDriver = TopologyTestDriver(topology, props)

        inputTopic = testDriver.createInputTopic(
            SYFO_STATUS_DIALOGMELDING_TOPIC,
            Serdes.String().serializer(),
            dialogmeldingStatusDTOSerde().serializer()
        )
    }

    @AfterEach
    fun afterEach() {
        InitTestDatabase.clean()
    }

    companion object {
        val dataSource = InitTestDatabase.dataSource
        val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())
        val util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron() != null }.map { it.type() })

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

    @Test
    fun kanTaImotStatusOppdateringer() {
        val uuid = UUID.randomUUID()
        val saksnummer = "saksnummer"
        val existingRecord = DialogmeldingRecord(uuid, "behandlerRef", "personIdent", saksnummer, DokumentasjonType.L8, "behandlernavn", "veiledernavn", "fritekst", UUID.randomUUID())
        setupRepositoryData(existingRecord)

        val incomingRecord = DialogmeldingStatusDTO(uuid.toString(), OffsetDateTime.now(), MeldingStatusType.BESTILT, "tekst", uuid.toString())

        inputTopic.pipeInput("key", incomingRecord)
        util.ventPåSvar()
        val oppdatertHendelse = hentRepositoryData(saksnummer)

        assertEquals(MeldingStatusType.BESTILT, oppdatertHendelse[0].status)
    }

    @Test
    fun kanSerialisereOgDeserialisere() {
        val dto = DialogmeldingStatusDTO(
            uuid = UUID.randomUUID().toString(),
            createdAt = OffsetDateTime.now(),
            status = MeldingStatusType.BESTILT,
            tekst = "tekst",
            bestillingUuid = UUID.randomUUID().toString()
        )

        val serde = dialogmeldingStatusDTOSerde()

        val serialized = serde.serializer().serialize(SYFO_STATUS_DIALOGMELDING_TOPIC, dto)
        val deserialized = serde.deserializer().deserialize(SYFO_STATUS_DIALOGMELDING_TOPIC, serialized)

        assertEquals(dto, deserialized)
    }

    private fun setupRepositoryData(record: DialogmeldingRecord) {
        InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.opprettDialogmelding(record)
        }
    }

    private fun hentRepositoryData(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        return InitTestDatabase.dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            dialogmeldingRepository.hentBySaksnummer(saksnummer)
        }
    }
}