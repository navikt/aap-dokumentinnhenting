package dokumentinnhenting.syfo

import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestillingService
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.random.Random

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlerDialogmeldingBestillingServiceTest {
    private lateinit var behandlerDialogmeldingBestillingService: BehandlerDialogmeldingBestillingService
    private lateinit var dialogmeldingRepository: DialogmeldingRepository

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `feiler om purring mangler tilhørende legeerklæring`() {
        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            assertThrows<RuntimeException> {
                behandlerDialogmeldingBestillingService.sendPåminnelseForBestilling(
                    randomUUID()
                )
            }
        }
    }

    @Test
    fun `feiler om man prøver å purre på legeerklæring som er under 14 dager gammel`() {
        lateinit var dialogmeldingLegerklæringUuid: UUID
        val saksnummer = Random.nextLong().toString()
        val legeerklæring = BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L8,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = UUID.randomUUID(),
            behandlerHprNr = "1233321"
        )

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection = connection
            )

            dialogmeldingLegerklæringUuid =
                behandlerDialogmeldingBestillingService.dialogmeldingBestilling(legeerklæring)
            dialogmeldingRepository.leggTilJournalpostPåBestilling(
                dialogmeldingLegerklæringUuid,
                "journalpostid",
                "dokumentid"
            )
        }

        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(
                connection = connection
            )

            assertThrows<RuntimeException> {
                behandlerDialogmeldingBestillingService.sendPåminnelseForBestilling(
                    dialogmeldingLegerklæringUuid
                )
            }
        }
    }

    @Test
    fun `sender automatisk purring når bestilling er 3 uker og 1 dag gammel`() {
        val (behandlingsReferanse, saksnummer, dialogmeldingUuid) = opprettForespørselOmLegeerklæringForTreUkerOgEnDagSiden()


        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            behandlerDialogmeldingBestillingService.sendAutomatiskPåminnelseHvisBestillingFinnes(
                BehandlingReferanse(behandlingsReferanse)
            )

            val lagretBestillinger = dialogmeldingRepository.hentBySaksnummer(saksnummer)

            val purring = lagretBestillinger.find {
                it.dokumentasjonType == DokumentasjonType.PURRING
            }
            assertThat(purring).isNotNull
            assertThat(purring!!.tidligereBestillingReferanse).isEqualTo(dialogmeldingUuid)
        }
    }

    @Test
    fun `sender ikke automatisk påminnelse når det allerede finnes en påminnelse for bestilling`() {
        val (behandlingsReferanse, saksnummer, dialogmeldingUuid) = opprettForespørselOmLegeerklæringForTreUkerOgEnDagSiden()

        dataSource.transaction { connection ->
            // lager manuell påminnelse
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)
            behandlerDialogmeldingBestillingService.sendPåminnelseForBestilling(
                dialogmeldingUuid
            )

            val påminnelserFørAutomatisk = dialogmeldingRepository.hentBySaksnummer(saksnummer)
                .filter { it.dokumentasjonType == DokumentasjonType.PURRING }
            assertThat(påminnelserFørAutomatisk).hasSize(1)

            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            behandlerDialogmeldingBestillingService.sendAutomatiskPåminnelseHvisBestillingFinnes(
                BehandlingReferanse(behandlingsReferanse)
            )

            // skal ikke opprette ny påminnelse hvis det allerede finnes en
            val lagretBestillinger = dialogmeldingRepository.hentBySaksnummer(saksnummer)
            val påminnelserEtterAutomatisk = lagretBestillinger.filter {
                it.dokumentasjonType == DokumentasjonType.PURRING
            }
            assertThat(påminnelserEtterAutomatisk.size).isEqualTo(1)
            assertThat(påminnelserEtterAutomatisk.first().tidligereBestillingReferanse).isEqualTo(dialogmeldingUuid)
        }
    }


    @Test
    fun `sender ikke purring når manuelt avbrutt`() {
        val (behandlingsReferanse, saksnummer, bestillingUuid) = opprettForespørselOmLegeerklæringForTreUkerOgEnDagSiden()

        // setter påminnelse avbrutt
        dataSource.transaction { connection ->
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)
            behandlerDialogmeldingBestillingService.avbrytPåminnelseForBestilling(bestillingUuid)
        }


        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            behandlerDialogmeldingBestillingService.sendAutomatiskPåminnelseHvisBestillingFinnes(
                BehandlingReferanse(behandlingsReferanse)
            )

            // skal ikke opprette ny påminnelse hvis manuelt avbrutt
            val lagretBestillinger = dialogmeldingRepository.hentBySaksnummer(saksnummer)
            val påminnelserEtterAutomatisk = lagretBestillinger.filter {
                it.dokumentasjonType == DokumentasjonType.PURRING
            }
            assertThat(påminnelserEtterAutomatisk).isEmpty()
        }
    }

    @Test
    fun `skal sende påminnelse når manuelt avbrutt og så gjenopptatt`() {
        val (behandlingsReferanse, saksnummer, bestillingUuid) = opprettForespørselOmLegeerklæringForTreUkerOgEnDagSiden()

        // setter påminnelse avbrutt
        dataSource.transaction { connection ->
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)
            behandlerDialogmeldingBestillingService.avbrytPåminnelseForBestilling(bestillingUuid)
        }

        // setter purring gjenopptatt
        dataSource.transaction { connection ->
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)
            behandlerDialogmeldingBestillingService.gjenopptaPåminnelseForBestilling(bestillingUuid)
        }


        dataSource.transaction { connection ->
            dialogmeldingRepository = DialogmeldingRepository(connection)
            behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)

            behandlerDialogmeldingBestillingService.sendAutomatiskPåminnelseHvisBestillingFinnes(
                BehandlingReferanse(behandlingsReferanse)
            )

            // skal opprette ny purring hvis manuelt avbrutt og så gjenopptatt
            val lagretBestillinger = dialogmeldingRepository.hentBySaksnummer(saksnummer)
            val påminnelserEtterAutomatisk = lagretBestillinger.filter {
                it.dokumentasjonType == DokumentasjonType.PURRING
            }
            assertThat(påminnelserEtterAutomatisk).isNotEmpty()
            assertThat(påminnelserEtterAutomatisk.first().tidligereBestillingReferanse).isEqualTo(bestillingUuid)
        }
    }

    private fun oppdaterOpprettetTidspunktForDialogmeldingRecord(tidspunkt: LocalDateTime, dialogmeldingUuid: UUID) {
        dataSource.transaction { connection ->
            connection.execute(
                """UPDATE DIALOGMELDING SET OPPRETTET_TID = ? WHERE DIALOGMELDING_UUID = ?""".trimIndent()
            ) {
                setParams {
                    setLocalDateTime(1, tidspunkt)
                    setUUID(2, dialogmeldingUuid)
                }
            }
        }
    }

    private fun opprettForespørselOmLegeerklæringForTreUkerOgEnDagSiden(): Triple<UUID, String, UUID> {
        val behandlingsReferanse = randomUUID()
        val saksnummer = Random.nextLong().toString()
        val legeerklæring = BehandlingsflytToDokumentInnhentingBestillingDto(
            bestillerNavIdent = "bestillerNavIdent",
            behandlerRef = "behandlerRef",
            personIdent = "12345678910",
            personNavn = "personNavn",
            saksnummer = saksnummer,
            dialogmeldingTekst = "tekst",
            dokumentasjonType = no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L40,
            behandlerNavn = "behandlerNavn",
            behandlingsReferanse = behandlingsReferanse,
            behandlerHprNr = "12344321"
        )

        val dialogmeldingUuid = dataSource.transaction { connection ->
            val behandlerDialogmeldingBestillingService = BehandlerDialogmeldingBestillingService(connection)
            behandlerDialogmeldingBestillingService.dialogmeldingBestilling(legeerklæring)
        }

        val treUkerOgEnDagSiden = LocalDate.now().minusWeeks(3).minusDays(1).atStartOfDay()
        oppdaterOpprettetTidspunktForDialogmeldingRecord(treUkerOgEnDagSiden, dialogmeldingUuid)
        return Triple(behandlingsReferanse, saksnummer, dialogmeldingUuid)
    }
}
