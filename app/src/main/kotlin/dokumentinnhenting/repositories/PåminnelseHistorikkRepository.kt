package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseHistorikkRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.util.NavIdent
import no.nav.aap.komponenter.repository.Repository
import java.time.LocalDateTime

interface PåminnelseHistorikkRepository : Repository {
    fun lagreNyPåminnelseStatusEndring(påminnelse: PåminnelseRecord, opprettetAvIdent: NavIdent, endringTidspunkt: LocalDateTime)
    fun hentPåminnelsePåPåminnelseId(påminnelseId: Long): List<PåminnelseHistorikkRecord>
}