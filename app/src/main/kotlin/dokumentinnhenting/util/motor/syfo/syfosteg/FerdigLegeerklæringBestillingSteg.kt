package dokumentinnhenting.util.motor.syfo.syfosteg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class FerdigLegeerklæringBestillingSteg: SyfoSteg.Utfører {
    private val log = LoggerFactory.getLogger(FerdigLegeerklæringBestillingSteg::class.java)

    override fun utfør(kontekst: SyfoSteg.Kontekst): SyfoSteg.Resultat {
        log.info("Prosessering har fullført.")
        return SyfoSteg.Resultat.FULLFØRT
    }

    companion object : SyfoSteg {
        override fun konstruer(connection: DBConnection): SyfoSteg.Utfører {
            return FerdigLegeerklæringBestillingSteg()
        }
    }
}