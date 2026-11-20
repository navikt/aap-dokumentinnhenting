package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseHistorikkRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

interface PåminnelseHistorikkRepository : Repository {
    fun lagreNyPåminnelseStatusEndring(påminnelse: PåminnelseRecord, opprettetAvIdent: Bruker, endringTidspunkt: LocalDateTime)
    fun hentPåminnelserPåPåminnelseId(påminnelseId: Long): List<PåminnelseHistorikkRecord>
}