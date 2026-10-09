package dokumentinnhenting.integrasjoner.saf

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SafGatewayTest {

    @Test
    fun `henter og samler alle sider`(): Unit = runBlocking {
        val førsteJournalpost = mockk<Journalpost>()
        val andreJournalpost = mockk<Journalpost>()
        val etterParametere = mutableListOf<String?>()

        val journalposter = hentAlleBrukerDokumentoversiktSider { etter ->
            etterParametere.add(etter)
            when (etter) {
                // SAF returns a cursor when another page is available.
                null -> DokumentoversiktBruker(
                    journalposter = listOf(førsteJournalpost),
                    sideInfo = SideInfo(sluttpeker = "side-1", finnesNesteSide = true),
                )

                // The cursor from page 1 is used to fetch this final page.
                "side-1" -> DokumentoversiktBruker(
                    journalposter = listOf(andreJournalpost),
                    sideInfo = SideInfo(sluttpeker = null, finnesNesteSide = false),
                )

                else -> error("Uventet sluttpeker: $etter")
            }
        }

        // Verify both the cursor sequence and that results from every page are returned.
        assertThat(etterParametere).containsExactly(null, "side-1")
        assertThat(journalposter).containsExactly(førsteJournalpost, andreJournalpost)
    }

    @Test
    fun `feiler eksplisitt når SAF mangler sluttpeker for neste side`() {
        assertThrows(InternfeilException::class.java) {
            runBlocking {
                hentAlleBrukerDokumentoversiktSider {
                    DokumentoversiktBruker(
                        journalposter = emptyList(),
                        sideInfo = SideInfo(sluttpeker = null, finnesNesteSide = true),
                    )
                }
            }
        }
    }

    @Test
    fun `feiler eksplisitt når SAF gjentar sluttpeker`() {
        assertThrows(InternfeilException::class.java) {
            runBlocking {
                hentAlleBrukerDokumentoversiktSider { _ ->
                    DokumentoversiktBruker(
                        journalposter = emptyList(),
                        sideInfo = SideInfo(sluttpeker = "samme-side", finnesNesteSide = true),
                    )
                }
            }
        }
    }
}
