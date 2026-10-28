package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import dokumentinnhenting.randomNavIdent
import dokumentinnhenting.randomNumeriskId
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
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
    fun `opprettPåminnelse oppretter påminnelse`() {
        val dialogmelding = lagDialogmeldingRecord()

        val returnertDialogmeldingId = dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(dialogmelding)
        }

        val påminnelse = lagPåminnelseRecord(id = 1, dialogmeldingId = returnertDialogmeldingId)

        dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.opprettPåminnelseVedBehov(påminnelse)
        }

        val lagretPåminnelse = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldingId(returnertDialogmeldingId).firstOrNull()
        }

        assertThat(lagretPåminnelse).isEqualTo(påminnelse.copy(id = lagretPåminnelse?.id ?: -1))
    }

    @Test
    fun `opprettPåminnelse oppretter ikke duplikat for påminnelse med samme dialogmeldingId`() {
        val dialogmelding = lagDialogmeldingRecord()

        val returnertDialogmeldingId = dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(dialogmelding)
        }

        val påminnelseOpprinnelig = lagPåminnelseRecord(id = 1, dialogmeldingId = returnertDialogmeldingId)
        val påminnelseDuplikat = lagPåminnelseRecord(id = 2, dialogmeldingId = returnertDialogmeldingId)

        dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.opprettPåminnelseVedBehov(påminnelseOpprinnelig)
            påminnelseRepository.opprettPåminnelseVedBehov(påminnelseDuplikat)
        }

        val lagredePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldingId(dialogmelding.dialogmeldingUuid)
        }

        assertThat(lagredePåminnelser).hasSize(1)
        assertThat(lagredePåminnelser.firstOrNull())?.isEqualTo(påminnelseOpprinnelig.copy(id = lagredePåminnelser.firstOrNull()?.id ?: -1))
    }

    @Test
    fun `hentPåminnelseForDialogmeldingId henter påminnelse med forespurt dialogmelding UUID`() {
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
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse1)
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse2)
            påminnelseRepository.opprettPåminnelseVedBehov(annenPåminnelse)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForDialogmeldingId(forespurtDialogmelding.dialogmeldingUuid)
        }

        assertThat(returnertePåminnelser).hasSize(1)
        assertThat(returnertePåminnelser.map { it.påminnelseDato })
            .containsExactlyInAnyOrder(forespurtPåminnelse1.påminnelseDato)
    }

    @Test
    fun `hentPåminnelseForDialogmeldinger henter alle påminnelser med dialogmelding UUID fra listen`() {
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
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse1)
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse2)
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse)
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

    @Test
    fun `hentPåminnelseForBehandlingsReferanse henter påminnelse med forespurt dialogmelding UUID`() {
        val behandlingsReferanse = randomUUID()
        val førsteDialogmelding = lagDialogmeldingRecord()
        val andreDialogmelding = lagDialogmeldingRecord()
        val tredjeDialogmelding = lagDialogmeldingRecord()

        val forespurtPåminnelseDato1 = LocalDate.now().plusDays(10)
        val forespurtPåminnelseDato2 = LocalDate.now().plusDays(15)
        val forespurtPåminnelse1 = lagPåminnelseRecord(
            dialogmeldingId = førsteDialogmelding.dialogmeldingUuid,
            påminnelseDato = forespurtPåminnelseDato1,
            behandlingsreferanse = behandlingsReferanse
        )
        val forespurtPåminnelse2 = lagPåminnelseRecord(
            dialogmeldingId = andreDialogmelding.dialogmeldingUuid,
            påminnelseDato = forespurtPåminnelseDato2,
            behandlingsreferanse = behandlingsReferanse
        )
        val annenPåminnelse = lagPåminnelseRecord(
            dialogmeldingId = tredjeDialogmelding.dialogmeldingUuid
        )

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(førsteDialogmelding)
            dialogmeldingRepository.opprettDialogmelding(andreDialogmelding)
            dialogmeldingRepository.opprettDialogmelding(tredjeDialogmelding)
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse1)
            påminnelseRepository.opprettPåminnelseVedBehov(forespurtPåminnelse2)
            påminnelseRepository.opprettPåminnelseVedBehov(annenPåminnelse)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            påminnelseRepository.hentPåminnelseForBehandlingsReferanse(BehandlingReferanse(behandlingsReferanse))
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser).containsExactlyInAnyOrder(
            forespurtPåminnelse1.copy(id = returnertePåminnelser[0].id),
            forespurtPåminnelse2.copy(id = returnertePåminnelser[1].id))
    }

    @Test
    fun `oppdaterPåminnelseStatus endrer status på returnert påminnelse`() {
        val førsteDialogmelding = lagDialogmeldingRecord()
        val andreDialogmelding = lagDialogmeldingRecord()
        val påminnelseSkalEndres = lagPåminnelseRecord(dialogmeldingId = førsteDialogmelding.dialogmeldingUuid)
        val påminnelseSkalIkkeEndres = lagPåminnelseRecord(dialogmeldingId = andreDialogmelding.dialogmeldingUuid)

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(førsteDialogmelding)
            dialogmeldingRepository.opprettDialogmelding(andreDialogmelding)
            påminnelseRepository.opprettPåminnelseVedBehov(påminnelseSkalEndres)
            påminnelseRepository.opprettPåminnelseVedBehov(påminnelseSkalIkkeEndres)

            påminnelseRepository.oppdaterPåminnelseStatus(påminnelseSkalEndres.dialogmeldingId, PåminnelseStatus.SENDT)
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            val førstePåminnelse = påminnelseRepository.hentPåminnelseForDialogmeldingId(førsteDialogmelding.dialogmeldingUuid)
            val andrePåminnelse = påminnelseRepository.hentPåminnelseForDialogmeldingId(andreDialogmelding.dialogmeldingUuid)
            førstePåminnelse + andrePåminnelse
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser[0]).isEqualTo(
            påminnelseSkalEndres.copy(id = returnertePåminnelser[0].id, status = PåminnelseStatus.SENDT)
        )
        assertThat(returnertePåminnelser[1]).isEqualTo(
            påminnelseSkalIkkeEndres.copy(id = returnertePåminnelser[1].id, status = PåminnelseStatus.PLANLAGT)
        )
    }

    @Test
    fun `oppdaterPåminnelseDato endrer dato på returnert påminnelse`() {
        val førsteDialogmelding = lagDialogmeldingRecord()
        val andreDialogmelding = lagDialogmeldingRecord()
        val påminnelseSkalEndres = lagPåminnelseRecord(
            dialogmeldingId = førsteDialogmelding.dialogmeldingUuid,
        )
        val påminnelseSkalIkkeEndres = lagPåminnelseRecord(
            dialogmeldingId = andreDialogmelding.dialogmeldingUuid,
        )

        dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(førsteDialogmelding)
            dialogmeldingRepository.opprettDialogmelding(andreDialogmelding)
            påminnelseRepository.opprettPåminnelseVedBehov(påminnelseSkalEndres)
            påminnelseRepository.opprettPåminnelseVedBehov(påminnelseSkalIkkeEndres)

            påminnelseRepository.oppdaterPåminnelseDato(
                påminnelseSkalEndres.dialogmeldingId,
                LocalDate.now().plusDays(100)
            )
        }

        val returnertePåminnelser = dataSource.transaction { connection ->
            val påminnelseRepository = PåminnelseRepositoryImpl(connection)

            val førstePåminnelse = påminnelseRepository.hentPåminnelseForDialogmeldingId(førsteDialogmelding.dialogmeldingUuid)
            val andrePåminnelse = påminnelseRepository.hentPåminnelseForDialogmeldingId(andreDialogmelding.dialogmeldingUuid)
            førstePåminnelse + andrePåminnelse
        }

        assertThat(returnertePåminnelser).hasSize(2)
        assertThat(returnertePåminnelser[0]).isEqualTo(
            påminnelseSkalEndres.copy(
                id = returnertePåminnelser[0].id,
                påminnelseDato = returnertePåminnelser[0].påminnelseDato
            )
        )
        assertThat(returnertePåminnelser[1]).isEqualTo(
            påminnelseSkalIkkeEndres.copy(
                id = returnertePåminnelser[1].id,
                påminnelseDato = returnertePåminnelser[1].påminnelseDato
            )
        )
    }

    private fun lagDialogmeldingRecord(
        uuid: UUID = randomUUID(),
        behandlingsreferanse: UUID = randomUUID(),
        saksnummer: String = "SAK-001",
        tidligereBestillingReferanse: UUID? = null,
        personIdent: String = randomNavIdent(),
        samtaleRef: UUID = randomUUID(),
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