package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDate
import java.util.UUID

interface PåminnelseRepository : Repository {
    fun opprettPåminnelseVedBehov(påminnelse: PåminnelseRecord)
    fun hentPåminnelseForId(påminnelseId: Long): PåminnelseRecord?
    fun hentPåminnelseForDialogmeldingId(dialogmeldingId: UUID): List<PåminnelseRecord>
    fun hentPåminnelseForDialogmeldinger(dialogmeldingIdListe: List<UUID>): List<PåminnelseRecord>
    fun hentPåminnelseForBehandlingsReferanse(behandlingsReferanse: BehandlingReferanse): List<PåminnelseRecord>
    fun oppdaterPåminnelseStatus(dialogmeldingId: UUID, status: PåminnelseStatus)
    fun oppdaterPåminnelseDato(dialogmeldingId: UUID, dato: LocalDate)
}