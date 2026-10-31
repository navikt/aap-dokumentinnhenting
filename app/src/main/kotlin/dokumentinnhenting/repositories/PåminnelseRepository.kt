package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface PåminnelseRepository : Repository {
    fun opprettPåminnelseEllerOppdaterStatus(påminnelse: PåminnelseRecord)
    fun opprettPåminnelseListeEllerOppdaterStatus(påminnelser: List<PåminnelseRecord>)
    fun hentPåminnelseForId(påminnelseId: Long): PåminnelseRecord?
    fun hentPåminnelseForDialogmeldingId(dialogmeldingId: UUID): List<PåminnelseRecord>
    fun hentPåminnelseForDialogmeldinger(dialogmeldingIdListe: List<UUID>): List<PåminnelseRecord>
    fun hentPåminnelseForBehandlingsReferanse(behandlingsReferanse: BehandlingReferanse): List<PåminnelseRecord>
    fun oppdaterPåminnelseStatus(dialogmeldingId: UUID, status: PåminnelseStatus)
    fun setForespørselSendt(dialogmeldingId: UUID, tidspunkt: LocalDateTime)
    fun oppdaterPåminnelseDato(dialogmeldingId: UUID, dato: LocalDate)
}