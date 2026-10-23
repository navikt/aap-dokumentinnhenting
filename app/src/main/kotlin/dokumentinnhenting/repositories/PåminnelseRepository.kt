package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import no.nav.aap.komponenter.repository.Repository
import java.util.UUID

interface PåminnelseRepository : Repository {
    fun hentPåminnelseForDialogmeldingId(dialogmeldingId: UUID): List<PåminnelseRecord>
    fun hentPåminnelseForDialogmeldinger(dialogmeldingIdListe: List<UUID>): List<PåminnelseRecord>
}