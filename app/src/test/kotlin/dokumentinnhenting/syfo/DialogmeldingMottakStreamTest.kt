package dokumentinnhenting.syfo

import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.*
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import dokumentinnhenting.integrasjoner.syfo.status.*
import org.junit.jupiter.api.Assertions.assertEquals
import no.nav.aap.komponenter.dbconnect.transaction
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.TestInputTopic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class DialogmeldingMottakStreamTest {
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, DialogmeldingMottakDTO>
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

    @BeforeEach
    fun setup() {
        InitTestDatabase.migrate()
        val dataSource = InitTestDatabase.dataSource

        val dialogmeldingStatusStream = DialogmeldingMottakStream(dataSource)

        val topology = dialogmeldingStatusStream.topology

        val props = Properties().apply {
            put("app.yapp", "app")
            put("bootstrap.servers", "dummy:1234")
        }

        testDriver = TopologyTestDriver(topology, props)

        inputTopic = testDriver.createInputTopic(
            SYFO_DIALOGMELDING_MOTTAK_TOPIC,
            Serdes.String().serializer(),
            dialogmeldingMottakDTOSerde().serializer()
        )
    }

    @AfterEach
    fun afterEach() {
        InitTestDatabase.clean()
    }

    @Test
    fun kanTaImotDialogmeldinger() {
        val uuid = UUID.randomUUID()
        val saksnummer = "saksnummer"
        val existingRecord = DialogmeldingRecord(uuid, "behandlerRef", "personIdent", saksnummer, DokumentasjonType.L8, "behandlernavn", "veiledernavn", "fritekst", UUID.randomUUID())
        setupRepositoryData(existingRecord)

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
                    TemaKode("kodeverkOID", "dn", "v","kat","kod","tittel"),
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
        val oppdatertHendelse = hentRepositoryData(saksnummer)
        assertEquals(uuid, oppdatertHendelse[0].dialogmeldingUuid)
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