package dokumentinnhenting.syfo

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.integrasjoner.syfo.SYFO_STATUS_DIALOGMELDING_TOPIC
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.createDialogmeldingStreamTopology
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDto
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.util.kafka.createGenericSerde
import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører
import dokumentinnhenting.util.motor.syfo.OppdaterLegeerklæringStatusUtfører.Companion.MOTTATT_TIDSPUNKT_PARAMETER
import dokumentinnhenting.util.motor.syfo.SendVarslingsbrevUtfører
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Properties
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DialogmeldingStatusHistorikkTest {
    private lateinit var dataSource: TestDataSource
    private val mottatt = Instant.parse("2026-10-23T11:02:56.123456Z")

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `lagrer historikk på konkret bestilling og purring med tidssonebevisste tider og valgfri tekst`() {
        val bestilling = opprettBestilling()
        val purring = opprettBestilling(DokumentasjonType.PURRING, bestilling)
        val hendelse = status(bestilling).copy(uuid = "isyfo-hendelse", tekst = "Status fra ISYFO")
        val purringHendelse = status(purring).copy(tekst = null)

        dataSource.transaction { connection ->
            connection.execute("SET LOCAL TIME ZONE 'Asia/Kolkata'")
            val repository = DialogmeldingRepositoryImpl(connection)
            repository.lagreStatusHistorikk(hendelse, mottatt)
            repository.lagreStatusHistorikk(purringHendelse, mottatt)
        }

        assertThat(historikk(bestilling)).containsExactly(
            Historikk(hendelse.uuid, bestilling, hendelse.status, hendelse.tekst, hendelse.createdAt.toInstant(), mottatt)
        )
        assertThat(historikk(purring)).containsExactly(
            Historikk(purringHendelse.uuid, purring, purringHendelse.status, null, purringHendelse.createdAt.toInstant(), mottatt)
        )
        val tidskolonner = dataSource.transaction { connection ->
            connection.queryList(
                """SELECT data_type FROM information_schema.columns
                   WHERE table_name = 'dialogmelding_status_historikk' AND column_name LIKE '%tidspunkt'"""
            ) {
                setRowMapper { it.getString("data_type") }
            }
        }
        assertThat(tidskolonner).containsExactly("timestamp with time zone", "timestamp with time zone")
    }

    @Test
    fun `nye hendelser med lik status lagres mens duplikat fortsatt oppdaterer gjeldende tekst`() {
        val bestilling = opprettBestilling()
        val første = status(bestilling)
        val andre = første.copy(uuid = UUID.randomUUID().toString(), tekst = null)
        utfør(jobb(første))
        utfør(jobb(andre))
        utfør(jobb(første.copy(tekst = "Tekst fra duplikat"), mottatt.plusSeconds(60)))

        assertThat(historikk(bestilling).map { it.hendelseId }).containsExactly(første.uuid, andre.uuid)
        assertThat(historikk(bestilling).first().mottattTidspunkt).isEqualTo(mottatt)
        dataSource.transaction { connection ->
            val lagret = requireNotNull(DialogmeldingRepositoryImpl(connection).hentByDialogId(bestilling))
            assertThat(lagret.status).isEqualTo(MeldingStatusType.SENDT)
            assertThat(lagret.statusTekst).isEqualTo("Tekst fra duplikat")
        }
    }

    @Test
    fun `duplikater og omordnede hendelser endrer ikke OK-sideeffekter eller statusregler`() {
        val bestilling = opprettBestilling()
        val ok = status(bestilling, MeldingStatusType.OK)
        val sendt = status(bestilling).copy(createdAt = ok.createdAt.plusDays(1))
        val brevGateway = mockk<BrevGateway>(relaxed = true)

        utfør(jobb(ok), brevGateway)
        utfør(jobb(ok), brevGateway)
        utfør(jobb(sendt), brevGateway)
        utfør(jobb(ok, mottatt.plusSeconds(60)), brevGateway)

        coVerify(exactly = 2) { brevGateway.ekspederBestilling(any()) }
        assertThat(varslingsjobber(bestilling)).hasSize(2)
        assertThat(historikk(bestilling).map { it.hendelseId }).containsExactly(ok.uuid, sendt.uuid)
        dataSource.transaction { connection ->
            assertThat(DialogmeldingRepositoryImpl(connection).hentByDialogId(bestilling)?.status)
                .isEqualTo(MeldingStatusType.OK)
        }
    }

    @Test
    fun `OK på purring ekspederer uten å varsle bruker`() {
        val bestilling = opprettBestilling(DokumentasjonType.PURRING, opprettBestilling())
        val brevGateway = mockk<BrevGateway>(relaxed = true)

        utfør(jobb(status(bestilling, MeldingStatusType.OK)), brevGateway)

        coVerify(exactly = 1) { brevGateway.ekspederBestilling(any()) }
        assertThat(varslingsjobber(bestilling)).isEmpty()
        assertThat(historikk(bestilling)).hasSize(1)
    }

    private fun opprettBestilling(
        type: DokumentasjonType = DokumentasjonType.L8,
        tidligereBestilling: UUID? = null
    ): UUID = dataSource.transaction { connection ->
        val repository = DialogmeldingRepositoryImpl(connection)
        val id = repository.opprettDialogmelding(
            DialogmeldingRecord(
                bestillerNavIdent = "Z123456",
                dialogmeldingUuid = UUID.randomUUID(),
                behandlerRef = "behandler",
                behandlerHprNr = "1234567",
                personIdent = "12345678901",
                personNavn = "Test Person",
                saksnummer = "SAK-STATUS",
                dokumentasjonType = type,
                behandlerNavn = "Test Behandler",
                fritekst = "Fritekst",
                behandlingsReferanse = UUID.randomUUID(),
                tidligereBestillingReferanse = tidligereBestilling,
                samtaleRef = UUID.randomUUID()
            )
        )
        repository.leggTilJournalpostPåBestilling(id, "1234", "5678")
        id
    }

    private fun status(bestilling: UUID, status: MeldingStatusType = MeldingStatusType.SENDT) =
        DialogmeldingStatusDto(
            uuid = UUID.randomUUID().toString(),
            createdAt = OffsetDateTime.parse("2026-10-20T13:45:12.654321-04:00"),
            status = status,
            tekst = "Statusmelding",
            bestillingUuid = bestilling.toString()
        )

    private fun jobb(hendelse: DialogmeldingStatusDto, mottattTidspunkt: Instant = mottatt) =
        JobbInput(OppdaterLegeerklæringStatusUtfører)
            .medPayload(DefaultJsonMapper.toJson(hendelse))
            .medParameter(MOTTATT_TIDSPUNKT_PARAMETER, mottattTidspunkt.toString())

    private fun utfør(input: JobbInput, brevGateway: BrevGateway = mockk()) {
        dataSource.transaction { connection ->
            OppdaterLegeerklæringStatusUtfører(
                DialogmeldingRepositoryImpl(connection), FlytJobbRepository(connection), brevGateway
            ).utfør(input)
        }
    }

    private fun historikk(bestilling: UUID): List<Historikk> = dataSource.transaction { connection ->
        connection.queryList(
            "SELECT * FROM DIALOGMELDING_STATUS_HISTORIKK WHERE DIALOGMELDING_UUID = ? ORDER BY ID"
        ) {
            setParams { setUUID(1, bestilling) }
            setRowMapper {
                Historikk(
                    it.getString("KILDEHENDELSE_ID"),
                    it.getUUID("DIALOGMELDING_UUID"),
                    it.getEnum("STATUS"),
                    it.getStringOrNull("TEKST"),
                    it.getInstant("HENDELSE_TIDSPUNKT"),
                    it.getInstantOrNull("MOTTATT_TIDSPUNKT")
                )
            }
        }
    }

    private fun varslingsjobber(bestilling: UUID): List<String> = dataSource.transaction { connection ->
        connection.queryList("SELECT PAYLOAD FROM JOBB WHERE TYPE = ? AND PAYLOAD LIKE ?") {
            setParams {
                setString(1, SendVarslingsbrevUtfører.type())
                setString(2, "%$bestilling%")
            }
            setRowMapper { it.getString("PAYLOAD") }
        }
    }

    private data class Historikk(
        val hendelseId: String,
        val dialogmeldingUuid: UUID,
        val status: MeldingStatusType,
        val tekst: String?,
        val hendelseTidspunkt: Instant,
        val mottattTidspunkt: Instant?
    )
}
