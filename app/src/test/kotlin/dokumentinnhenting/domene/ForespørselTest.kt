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

class ForespørselTest {
    @Test
    fun `forespørsel uten påminnelser har ingen siste påminnelse`() {
        val forespørsel = forespørsel(melding())

        assertThat(forespørsel.harPåminnelse()).isFalse()
        assertThat(forespørsel.sistePåminnelse()).isNull()
    }

    @Test
    fun `siste påminnelse velges etter opprettelsestidspunkt uavhengig av rekkefølge`() {
        val originalmelding = melding()
        val første = Påminnelse(melding(opprettet = originalmelding.opprettetTidspunkt.plusDays(22)), originalmelding.id)
        val siste = Påminnelse(melding(opprettet = originalmelding.opprettetTidspunkt.plusDays(25)), originalmelding.id)
        val forespørsel = forespørsel(originalmelding, listOf(siste, første))

        assertThat(forespørsel.harPåminnelse()).isTrue()
        assertThat(forespørsel.sistePåminnelse()).isEqualTo(siste)
        assertThat(siste.melding.gjeldendeStatus).isNull()
    }

    @Test
    fun `påminnelse må tilhøre forespørselen`() {
        val påminnelse = Påminnelse(melding(), UUID.randomUUID())

        assertThatThrownBy { forespørsel(melding(), listOf(påminnelse)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Alle påminnelser må referere til forespørselen")
    }

    @Test
    fun `forespørsel og påminnelse har egen status og historikk`() {
        val originalhistorikk = hendelse(MeldingStatusType.OK)
        val påminnelsehistorikk = hendelse(MeldingStatusType.SENDT)
        val originalmelding = melding().copy(
            gjeldendeStatus = MeldingStatusType.OK,
            statushistorikk = listOf(originalhistorikk),
        )
        val påminnelse = Påminnelse(
            melding().copy(
                gjeldendeStatus = MeldingStatusType.SENDT,
                statushistorikk = listOf(påminnelsehistorikk),
            ),
            originalmelding.id,
        )
        val forespørsel = forespørsel(originalmelding, listOf(påminnelse))

        assertThat(forespørsel.melding.statushistorikk).containsExactly(originalhistorikk)
        assertThat(forespørsel.påminnelser.single().melding.statushistorikk).containsExactly(påminnelsehistorikk)
        assertThat(forespørsel.melding.gjeldendeStatus).isEqualTo(MeldingStatusType.OK)
        assertThat(påminnelse.melding.gjeldendeStatus).isEqualTo(MeldingStatusType.SENDT)
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
    fun `forespørsel uten lagret plan har ingen påminnelsesplan`() {
        assertThat(forespørsel(melding()).påminnelsesplan).isNull()
    }

    @Test
    fun `påminnelsesplan må tilhøre forespørselen og behandlingen`() {
        val forespørsel = forespørsel(melding())
        val plan = Påminnelsesplan(
            forespørselId = forespørsel.melding.id,
            behandlingsreferanse = forespørsel.melding.behandlingsreferanse,
            påminnelsesdato = forespørsel.melding.opprettetTidspunkt.toLocalDate().plusDays(22),
            opprettetTidspunkt = forespørsel.melding.opprettetTidspunkt,
            tilstand = Påminnelsestilstand.Planlagt,
        )

        assertThat(forespørsel.copy(påminnelsesplan = plan).påminnelsesplan)
            .usingRecursiveComparison().isEqualTo(plan)
        assertThatThrownBy {
            forespørsel.copy(påminnelsesplan = plan.copy(forespørselId = UUID.randomUUID()))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Påminnelsesplanen må referere til forespørselen")
        assertThatThrownBy {
            forespørsel.copy(
                påminnelsesplan = plan.copy(behandlingsreferanse = BehandlingReferanse(UUID.randomUUID()))
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Påminnelsesplanen må tilhøre samme behandling som forespørselen")
    }

    private fun forespørsel(melding: UtgåendeDialogmelding, påminnelser: List<Påminnelse> = emptyList()) =
        Forespørsel(
            melding = melding,
            dokumentasjonstype = DokumentasjonType.L40,
            automatiskPåminnelseAktiv = true,
            påminnelser = påminnelser,
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
