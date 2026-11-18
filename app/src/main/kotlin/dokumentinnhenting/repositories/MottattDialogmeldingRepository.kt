package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import java.util.UUID

interface MottattDialogmeldingRepository {
    fun lagre(dialogmelding: DialogmeldingMottakDTO, saksnummer: String)
    fun eksisterer(dialogmeldingId: UUID): Boolean
    fun hentForSaksnummer(saksnummer: String): List<MottattDialogmeldingRecord>
}