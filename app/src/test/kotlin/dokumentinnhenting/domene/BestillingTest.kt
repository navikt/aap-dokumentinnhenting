package dokumentinnhenting.domene

import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class BestillingTest {
    @Test
    fun `bestilling uten purringer har ingen siste purring`() {
        val bestilling = bestilling(melding())

        assertThat(bestilling.harPurring()).isFalse()
        assertThat(bestilling.sistePurring()).isNull()
    }

    @Test
    fun `siste purring velges etter opprettelsestidspunkt uavhengig av rekkefølge`() {
        val originalmelding = melding()
        val første = Purring(melding(opprettet = originalmelding.opprettetTidspunkt.plusDays(22)), originalmelding.id)
        val siste = Purring(melding(opprettet = originalmelding.opprettetTidspunkt.plusDays(25)), originalmelding.id)
        val bestilling = bestilling(originalmelding, listOf(siste, første))

        assertThat(bestilling.harPurring()).isTrue()
        assertThat(bestilling.sistePurring()).isEqualTo(siste)
        assertThat(siste.melding.gjeldendeStatus).isNull()
    }

    @Test
    fun `purring må tilhøre originalbestillingen`() {
        val purring = Purring(melding(), UUID.randomUUID())

        assertThatThrownBy { bestilling(melding(), listOf(purring)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Alle purringer må referere til originalbestillingen")
    }

    @Test
    fun `bestilling og purring har egen status og historikk`() {
        val originalhistorikk = hendelse(MeldingStatusType.OK)
        val purringhistorikk = hendelse(MeldingStatusType.SENDT)
        val originalmelding = melding().copy(
            gjeldendeStatus = MeldingStatusType.OK,
            statushistorikk = listOf(originalhistorikk),
        )
        val purring = Purring(
            melding().copy(
                gjeldendeStatus = MeldingStatusType.SENDT,
                statushistorikk = listOf(purringhistorikk),
            ),
            originalmelding.id,
        )
        val bestilling = bestilling(originalmelding, listOf(purring))

        assertThat(bestilling.melding.statushistorikk).containsExactly(originalhistorikk)
        assertThat(bestilling.purringer.single().melding.statushistorikk).containsExactly(purringhistorikk)
        assertThat(bestilling.melding.gjeldendeStatus).isEqualTo(MeldingStatusType.OK)
        assertThat(purring.melding.gjeldendeStatus).isEqualTo(MeldingStatusType.SENDT)
    }

    @Test
    fun `gjeldende status kan finnes uten historikk for eldre meldinger`() {
        val melding = melding().copy(gjeldendeStatus = MeldingStatusType.OK)

        assertThat(melding.gjeldendeStatus).isEqualTo(MeldingStatusType.OK)
        assertThat(melding.statushistorikk).isEmpty()
    }

    @Test
    fun `journalreferanse bruker eksisterende journalpostidentifikator`() {
        val journalpostId = JournalpostId("123456")
        val melding = melding().copy(
            journalreferanse = Journalreferanse(journalpostId, "654321"),
        )

        assertThat(melding.journalreferanse?.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    fun `bestilling uten lagret plan har ingen påminnelsesplan`() {
        assertThat(bestilling(melding()).påminnelsesplan).isNull()
    }

    @Test
    fun `påminnelsesplan må tilhøre bestillingen og behandlingen`() {
        val bestilling = bestilling(melding())
        val plan = Påminnelsesplan(
            originalbestillingId = bestilling.melding.id,
            behandlingsreferanse = bestilling.melding.behandlingsreferanse,
            påminnelsesdato = bestilling.melding.opprettetTidspunkt.toLocalDate().plusDays(22),
            opprettetTidspunkt = bestilling.melding.opprettetTidspunkt,
            tilstand = Påminnelsestilstand.Planlagt,
        )

        assertThat(bestilling.copy(påminnelsesplan = plan).påminnelsesplan)
            .usingRecursiveComparison().isEqualTo(plan)
        assertThatThrownBy {
            bestilling.copy(påminnelsesplan = plan.copy(originalbestillingId = UUID.randomUUID()))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Påminnelsesplanen må referere til originalbestillingen")
        assertThatThrownBy {
            bestilling.copy(
                påminnelsesplan = plan.copy(behandlingsreferanse = BehandlingReferanse(UUID.randomUUID()))
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Påminnelsesplanen må tilhøre samme behandling som bestillingen")
    }

    private fun bestilling(melding: UtgåendeDialogmelding, purringer: List<Purring> = emptyList()) =
        Bestilling(
            melding = melding,
            dokumentasjonstype = DokumentasjonType.L40,
            automatiskPåminnelseAktiv = true,
            purringer = purringer,
        )

    private fun melding(opprettet: LocalDateTime = LocalDateTime.parse("2026-10-01T10:00:00")) =
        UtgåendeDialogmelding(
            id = UUID.randomUUID(),
            behandlingsreferanse = BehandlingReferanse(UUID.randomUUID()),
            saksnummer = Saksnummer("SAK-001"),
            person = Person("12345678901", "Testperson"),
            behandler = Behandler("behandler-ref", "1234567", "Testbehandler"),
            bestillerNavIdent = "Z123456",
            tekst = "Forespørsel",
            opprettetTidspunkt = opprettet,
            journalreferanse = null,
            gjeldendeStatus = null,
            statusTekst = null,
            samtaleRef = UUID.randomUUID(),
            flytStatus = null,
            statushistorikk = emptyList(),
        )

    private fun hendelse(status: MeldingStatusType) = Statushendelse(
        kildehendelseId = UUID.randomUUID().toString(),
        status = status,
        hendelseTidspunkt = Instant.parse("2026-10-01T08:00:00Z"),
        mottattTidspunkt = Instant.parse("2026-10-01T08:01:00Z"),
        tekst = null,
    )
}
