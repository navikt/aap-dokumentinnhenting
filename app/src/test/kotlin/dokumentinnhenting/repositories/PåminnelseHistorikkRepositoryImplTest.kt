package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import dokumentinnhenting.randomNavIdent
import dokumentinnhenting.util.NavIdent
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
class PåminnelseHistorikkRepositoryImplTest {

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `lagreNyPåminnelseTilstand lagrer tilstand`() {
        val opprettetAv = NavIdent("Behandler NAV")
        val endringTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        val dialogmelding = lagDialogmeldingRecord()
        val returnertDialogmeldingId = dataSource.transaction { connection ->
            val dialogmeldingRepository = DialogmeldingRepositoryImpl(connection)

            dialogmeldingRepository.opprettDialogmelding(dialogmelding)
        }

        val påminnelse = lagPåminnelseRecord(returnertDialogmeldingId)
        val lagretPåminnelse = dataSource.transaction { connection ->
            val repository = PåminnelseRepositoryImpl(connection)

            repository.opprettPåminnelseEllerOppdaterStatus(påminnelse)
            repository.hentPåminnelseForDialogmeldingId(returnertDialogmeldingId).first()
        }

        dataSource.transaction { connection ->
            val repository = PåminnelseHistorikkRepositoryImpl(connection)

            repository.lagreNyPåminnelseStatusEndring(lagretPåminnelse, opprettetAv, endringTidspunkt)
        }

        val lagretPåminnelseRecord = dataSource.transaction { connection ->
            val repository = PåminnelseHistorikkRepositoryImpl(connection)

            repository.hentPåminnelsePåPåminnelseId(lagretPåminnelse.id!!).firstOrNull()
        }

        assertThat(lagretPåminnelseRecord).isNotNull
        assertThat(lagretPåminnelseRecord?.påminnelseId).isEqualTo(lagretPåminnelse.id)
        assertThat(lagretPåminnelseRecord?.opprettetAv).isEqualTo(opprettetAv)
        assertThat(lagretPåminnelseRecord?.påminnelseStatus).isEqualTo(påminnelse.status)
        assertThat(lagretPåminnelseRecord?.hendelseTidspunkt).isEqualTo(endringTidspunkt)
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
        id: Long? = null,
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