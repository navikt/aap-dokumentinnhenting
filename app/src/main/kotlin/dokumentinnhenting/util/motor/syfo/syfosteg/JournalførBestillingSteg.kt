package dokumentinnhenting.util.motor.syfo.syfosteg

import dokumentinnhenting.integrasjoner.brev.BrevClient
import dokumentinnhenting.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory
import java.util.*

class JournalførBestillingSteg(
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val brevClient: BrevClient
): SyfoSteg.Utfører {
    private val log = LoggerFactory.getLogger(StartLegeerklæringBestillingSteg::class.java)

    override fun utfør(kontekst: SyfoSteg.Kontekst): SyfoSteg.Resultat {
        log.info("JournalførBestillingSteg")
        return sendBestilling(kontekst.referanse)
    }

    companion object : SyfoSteg {
        override fun konstruer(connection: DBConnection): SyfoSteg.Utfører {
            return JournalførBestillingSteg(
                DialogmeldingRepository(connection),
                BrevClient()
            )
        }
    }

    fun sendBestilling(dialogmeldingUuid: UUID): SyfoSteg.Resultat {
        val funnetBestilling = dialogmeldingRepository.hentByDialogId(dialogmeldingUuid)
        val tidligereTilhørendeBestillingsdato = funnetBestilling.tidligereBestillingReferanse?.let { dialogmeldingRepository.hentBestillingEldreEnn14Dager(it)?.opprettet }

        try {
            brevClient.journalførBestilling(funnetBestilling, tidligereTilhørendeBestillingsdato)
        } catch (e: Exception) {
            log.error("Feilet ved journalføring av dokument $dialogmeldingUuid", e)
            return SyfoSteg.Resultat.STOPP
        }

        return SyfoSteg.Resultat.FULLFØRT
    }
}