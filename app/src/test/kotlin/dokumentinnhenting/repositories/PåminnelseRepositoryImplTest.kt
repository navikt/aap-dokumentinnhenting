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

        val returnertDialogmeldingId = dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(dialogmelding)
        }

        val påminnelse = lagPåminnelseRecord(id = 1, dialogmeldingId = returnertDialogmeldingId)

        val returnertPåminnelseId = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.opprettPåminnelse(påminnelse)
        }

        val lagretPåminnelse = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForId(returnertPåminnelseId)
        }

        assertThat(lagretPåminnelse).isEqualTo(påminnelse.copy(id = returnertPåminnelseId))
    }

    @Test
    fun `hentPåminnelseForDialogmeldingId henter alle dialogmeldinger med forespurt dialogmelding UUID`() {
        val forespurtDialogmelding = lagDialogmeldingRecord()
        val annenDialogmelding = lagDialogmeldingRecord()

        val forespurtPåminnelseDato1 = LocalDate.now().plusDays(10)
        val forespurtPåminnelseDato2 = LocalDate.now().plusDays(15)
        val forespurtPåminnelse1 = lagPåminnelseRecord(
            dialogmeldingId = forespurtDialogmelding.dialogmeldingUuid,
            påminnelseDato = forespurtPåminnelseDato1
        )
        val forespurtPåminnelse2 = lagPåminnelseRecord(
            dialogmeldingId = forespurtDialogmelding.dialogmeldingUuid,
            påminnelseDato = forespurtPåminnelseDato2
        )
        val annenPåminnelse = lagPåminnelseRecord(
            dialogmeldingId = annenDialogmelding.dialogmeldingUuid
        )

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(forespurtDialogmelding)
            dialogmeldingRepository.opprettDialogmelding(annenDialogmelding)
            påminnelseRepository.opprettPåminnelse(forespurtPåminnelse1)
            påminnelseRepository.opprettPåminnelse(forespurtPåminnelse2)
            påminnelseRepository.opprettPåminnelse(annenPåminnelse)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldingId(forespurtDialogmelding.dialogmeldingUuid)
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser.map { it.påminnelseDato })
            .containsExactlyInAnyOrder(forespurtPåminnelse1.påminnelseDato, forespurtPåminnelse2.påminnelseDato)
    }

    @Test
    fun `hentPåminnelseForDialogmeldinger henter alle dialogmeldinger med dialogmelding UUID fra listen`() {
        val forespurtDialogmelding1 = lagDialogmeldingRecord()
        val forespurtDialogmelding2 = lagDialogmeldingRecord()
        val annenDialogmelding = lagDialogmeldingRecord()

        val forespurtPåminnelseDato1 = LocalDate.now().plusDays(10)
        val forespurtPåminnelseDato2 = LocalDate.now().plusDays(15)
        val annenPåminnelseDato = LocalDate.now().plusDays(20)
        val forespurtPåminnelse1 = lagPåminnelseRecord(
            forespurtDialogmelding1.dialogmeldingUuid,
            påminnelseDato = forespurtPåminnelseDato1
        )
        val forespurtPåminnelse2 = lagPåminnelseRecord(
            forespurtDialogmelding2.dialogmeldingUuid,
            påminnelseDato = forespurtPåminnelseDato2
        )
        val forespurtPåminnelse = lagPåminnelseRecord(
            annenDialogmelding.dialogmeldingUuid,
            påminnelseDato = annenPåminnelseDato
        )

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(forespurtDialogmelding1)
            dialogmeldingRepository.opprettDialogmelding(forespurtDialogmelding2)
            dialogmeldingRepository.opprettDialogmelding(annenDialogmelding)
            påminnelseRepository.opprettPåminnelse(forespurtPåminnelse1)
            påminnelseRepository.opprettPåminnelse(forespurtPåminnelse2)
            påminnelseRepository.opprettPåminnelse(forespurtPåminnelse)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldinger(
                listOf(forespurtDialogmelding1.dialogmeldingUuid, forespurtDialogmelding2.dialogmeldingUuid)
            )
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser.map { it.påminnelseDato })
            .containsExactlyInAnyOrder(forespurtPåminnelse1.påminnelseDato, forespurtPåminnelse2.påminnelseDato)
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