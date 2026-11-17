package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseHistorikkRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDateTime

interface PåminnelseHistorikkRepository : Repository {
    fun lagreNyPåminnelseTilstand(påminnelse: PåminnelseRecord, opprettetAvIdent: String, endringTidspunkt: LocalDateTime)
    fun hentPåminnelsePåPåminnelseId(påminnelseId: Long): List<PåminnelseHistorikkRecord>
}