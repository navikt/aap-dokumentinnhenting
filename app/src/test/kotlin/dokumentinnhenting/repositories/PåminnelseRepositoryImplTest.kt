package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import dokumentinnhenting.randomNavIdent
import dokumentinnhenting.randomNumeriskId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PåminnelseRepositoryImplTest {

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun teadown() {
        dataSource.close()
    }

    @Test
    fun `opprettPåminnelse oppretter påminnelse og returnerer Long`() {
        val dialogmelding = lagDialogmeldingRecord()
        val påminnelse = lagPåminnelseRecord(dialogmelding.dialogmeldingUuid)

        val returnertId = dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(dialogmelding)
            påminnelseRepository.opprettPåminnelse(påminnelse)
        }

        val returnertPåminnelse = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForId(returnertId)
        }

        assertEquals(returnertId, returnertPåminnelse?.id)
        assertEquals(påminnelse.dialogmeldingId, returnertPåminnelse?.dialogmeldingId)
        assertEquals(påminnelse.behandlingsReferanse, returnertPåminnelse?.behandlingsReferanse)
        assertEquals(påminnelse.status, returnertPåminnelse?.status)
        assertEquals(påminnelse.påminnelseDato, returnertPåminnelse?.påminnelseDato)
        assertEquals(påminnelse.opprettetTidspunkt, returnertPåminnelse?.opprettetTidspunkt)
        assertEquals(påminnelse.sisteForespørselSendt, returnertPåminnelse?.sisteForespørselSendt)
    }

    @Test
    fun `hentPåminnelseForDialogmeldingId henter all dialogmeldinger med riktig dialogmelding UUID`() {
        val riktigDialogmelding1 = lagDialogmeldingRecord()
        val annenDialogmelding2 = lagDialogmeldingRecord()

        val påminnelseDato1 = LocalDate.now().plusDays(10)
        val påminnelseDato2 = LocalDate.now().plusDays(15)
        val påminnelse1 = lagPåminnelseRecord(
            riktigDialogmelding1.dialogmeldingUuid,
            påminnelseDato = påminnelseDato1
        )
        val påminnelse2 = lagPåminnelseRecord(
            riktigDialogmelding1.dialogmeldingUuid,
            påminnelseDato = påminnelseDato2
        )
        val påminnelse3 = lagPåminnelseRecord(annenDialogmelding2.dialogmeldingUuid)

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(riktigDialogmelding1)
            dialogmeldingRepository.opprettDialogmelding(annenDialogmelding2)
            påminnelseRepository.opprettPåminnelse(påminnelse1)
            påminnelseRepository.opprettPåminnelse(påminnelse2)
            påminnelseRepository.opprettPåminnelse(påminnelse3)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldingId(riktigDialogmelding1.dialogmeldingUuid)
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser.map { it.påminnelseDato })
            .containsExactlyInAnyOrder(påminnelse1.påminnelseDato, påminnelse2.påminnelseDato)
    }

    @Test
    fun `hentPåminnelseForDialogmeldinger henter all dialogmeldinger med dialogmelding UUID fra listen`() {
        val riktigDialogmelding1 = lagDialogmeldingRecord()
        val riktigDialogmelding2 = lagDialogmeldingRecord()
        val annenDialogmelding = lagDialogmeldingRecord()

        val påminnelseDato1 = LocalDate.now().plusDays(10)
        val påminnelseDato2 = LocalDate.now().plusDays(15)
        val påminnelseDato3 = LocalDate.now().plusDays(20)
        val påminnelse1 = lagPåminnelseRecord(
            riktigDialogmelding1.dialogmeldingUuid,
            påminnelseDato = påminnelseDato1
        )
        val påminnelse2 = lagPåminnelseRecord(
            riktigDialogmelding2.dialogmeldingUuid,
            påminnelseDato = påminnelseDato2
        )
        val påminnelse3 = lagPåminnelseRecord(
            annenDialogmelding.dialogmeldingUuid,
            påminnelseDato = påminnelseDato3
        )

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(riktigDialogmelding1)
            dialogmeldingRepository.opprettDialogmelding(riktigDialogmelding2)
            dialogmeldingRepository.opprettDialogmelding(annenDialogmelding)
            påminnelseRepository.opprettPåminnelse(påminnelse1)
            påminnelseRepository.opprettPåminnelse(påminnelse2)
            påminnelseRepository.opprettPåminnelse(påminnelse3)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldinger(
                listOf(riktigDialogmelding1.dialogmeldingUuid, riktigDialogmelding2.dialogmeldingUuid)
            )
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser.map { it.påminnelseDato })
            .containsExactlyInAnyOrder(påminnelse1.påminnelseDato, påminnelse2.påminnelseDato)
    }

    private fun lagDialogmeldingRecord(
        uuid: UUID = UUID.randomUUID(),
        behandlingsreferanse: UUID = UUID.randomUUID(),
        saksnummer: String = "SAK-001",
        tidligereBestillingReferanse: UUID? = null,
        personIdent: String = randomNavIdent(),
        samtaleRef: UUID = UUID.randomUUID(),
        dokumentasjonType: DokumentasjonType = DokumentasjonType.L8
    ) = DialogmeldingRecord(
        bestillerNavIdent = randomNavIdent(),
        dialogmeldingUuid = uuid,
        behandlingsReferanse = behandlingsreferanse,
        behandlerRef = "behandlerRef-123",
        behandlerHprNr = "12344321",
        personIdent = personIdent,
        personNavn = "Ola Nordmann",
        saksnummer = saksnummer,
        dokumentasjonType = dokumentasjonType,
        behandlerNavn = "Dr. Behandler",
        fritekst = "En fritekst",
        tidligereBestillingReferanse = tidligereBestillingReferanse,
        samtaleRef = samtaleRef,
    )

    private fun lagPåminnelseRecord(
        dialogmeldingId: UUID,
        id: Long = randomNumeriskId(),
        behandlingsreferanse: UUID = randomUUID(),
        status: PåminnelseStatus = PåminnelseStatus.PLANLAGT,
        påminnelseDato: LocalDate = LocalDate.now().plusDays(22),
        opprettetTidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        sisteForespørselSendt: LocalDateTime? = null,
    ) = PåminnelseRecord(
        id = id,
        dialogmeldingId = dialogmeldingId,
        behandlingsReferanse = behandlingsreferanse,
        status = status,
        påminnelseDato = påminnelseDato,
        opprettetTidspunkt = opprettetTidspunkt,
        sisteForespørselSendt = sisteForespørselSendt
    )
}