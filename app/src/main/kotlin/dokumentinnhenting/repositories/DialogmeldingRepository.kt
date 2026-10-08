package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDto
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl.SyfoBestillingFlytStatus
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDate
import java.util.*

interface DialogmeldingRepository : Repository {
    fun opprettDialogmelding(melding: DialogmeldingRecord): UUID

    fun oppdaterDialogmeldingStatus(melding: DialogmeldingStatusDto)

    fun leggTilJournalpostPåBestilling(dialogmeldingUuid: UUID, journalpostId: String, dokumentId: String)

    fun oppdaterFlytStatus(dialogmeldingUuid: UUID, flytStatus: ProsesseringSyfoStatus)

    fun hentBestillingEldreEnn14Dager(dialogmeldingUuid: UUID): DialogmeldingFullRecord?

    fun hentBestillingerForDokumentasjonstyper(
        behandlingReferanse: BehandlingReferanse,
        dokumentasjonstyper: List<DokumentasjonType>
    ): List<DialogmeldingFullRecord>

    fun hentForSaksnummer(saksnummer: String): List<DialogmeldingFullRecord>
    fun hentByDialogId(dialogmeldingUuid: UUID): DialogmeldingFullRecord?

    fun hentForParent(parentRef: UUID, personIdent: String): DialogmeldingFullRecord?

    fun oppdaterPersonIdentPåSamtaleRef(samtaleRef: UUID, muligeIdenter: List<String>, nyIdent: String)

    fun oppdaterPersonIdentPåParentRef(parentRef: UUID, muligeIdenter: List<String>, nyIdent: String)

    fun eksisterer(dialogmeldingUuid: UUID): Boolean

    fun hentFlytStatus(dialogmeldingUuid: UUID): SyfoBestillingFlytStatus

    fun hentBestillingerSomSkalPåminnes(
        behandlingReferanse: BehandlingReferanse,
        dokumentasjonstype: DokumentasjonType,
        opprettetDato: LocalDate
    ): List<DialogmeldingFullRecord>

    fun settAutomatiskPåminnelse(automatiskPåminnelse: Boolean, dialogmeldingUuid: UUID)
    fun låsBestilling(dialogmeldingUuid: UUID): UUID
}