package dokumentinnhenting.util.motor.syfo.syfosteg

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class StartLegeerklæringBestillingSteg: SyfoSteg.Utfører {
    private val log = LoggerFactory.getLogger(StartLegeerklæringBestillingSteg::class.java)

    override fun utfør(kontekst: SyfoSteg.Kontekst): SyfoSteg.Resultat {
        log.info("Prosessering har startet.")
        return SyfoSteg.Resultat.FULLFØRT
    }

    companion object : SyfoSteg {
        override fun konstruer(connection: DBConnection): SyfoSteg.Utfører {
            return StartLegeerklæringBestillingSteg()
        }
    }
}