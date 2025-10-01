package dokumentinnhenting.util.dokument

import dokumentinnhenting.integrasjoner.saf.DokumentInfo
import dokumentinnhenting.integrasjoner.saf.Dokumentvariant
import dokumentinnhenting.integrasjoner.saf.Journalpost
import dokumentinnhenting.integrasjoner.saf.Journalposttype
import dokumentinnhenting.integrasjoner.saf.Journalstatus
import dokumentinnhenting.integrasjoner.saf.Variantformat
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class DokumentFunctionsTest {

    @Test
    fun `Ikke inkluder journalposter uten dokumenter`() {
        val journalposter = listOf(
            opprettJournalpost(opprettDokument(Variantformat.ARKIV)),
            opprettJournalpost(),
            opprettJournalpost(opprettDokument(Variantformat.ORIGINAL)),
        )

        assertEquals(3, journalposter.size)
        assertEquals(1, journalposter.mapKunVariantformatArkiv().size)
    }

    @Test
    fun `Skal kun inkludere journalposter hvor det finnes dokumentvarianter av type ARKIV`() {
        val journalposter = listOf(
            opprettJournalpost(opprettDokument(Variantformat.ARKIV)),
            opprettJournalpost(opprettDokument(Variantformat.ARKIV, Variantformat.ORIGINAL)),
            opprettJournalpost(opprettDokument(Variantformat.ORIGINAL)),
        )

        assertEquals(3, journalposter.size)
        assertEquals(2, journalposter.mapKunVariantformatArkiv().size)
    }

    private fun opprettJournalpost(vararg dokumenter: DokumentInfo) = Journalpost(
        journalpostId = UUID.randomUUID().toString(),
        dokumenter = dokumenter.toList(),
        tittel = null,
        journalposttype = Journalposttype.U,
        journalstatus = Journalstatus.MOTTATT,
        tema = null,
        temanavn = null,
        behandlingstema = null,
        behandlingstemanavn = null,
        sak = null,
        avsenderMottaker = null,
        datoOpprettet = null,
        relevanteDatoer = emptyList(),
    )

    private fun opprettDokument(vararg varianter: Variantformat) = mockk<DokumentInfo> {
        every { dokumentvarianter } returns varianter.map { Dokumentvariant(it, true) }
    }
}
