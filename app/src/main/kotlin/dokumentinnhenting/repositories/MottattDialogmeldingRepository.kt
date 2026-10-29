package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import java.util.UUID

interface MottattDialogmeldingRepository {
    fun lagre(dialogmelding: DialogmeldingMottakDTO, saksnummer: String)
    fun eksisterer(dialogmeldingId: UUID): Boolean
    fun hentForSamtale(samtaleRef: UUID, personIdent: String): List<MottattDialogmeldingRecord>
    fun hentForParent(parentRef: UUID, personIdent: String): List<MottattDialogmeldingRecord>
    fun hentForSaksnummer(saksnummer: String): List<MottattDialogmeldingRecord>
    fun oppdaterPersonIdentPåSamtaleRef(samtaleRef: UUID, muligeIdenter: List<String>, nyIdent: String)
    fun oppdaterPersonIdentPåParentRef(parentRef: UUID, muligeIdenter: List<String>, nyIdent: String)
}