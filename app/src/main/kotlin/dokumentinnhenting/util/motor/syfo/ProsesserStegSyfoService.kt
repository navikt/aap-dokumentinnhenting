package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.syfosteg.*
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory
import java.util.*

class ProsesserStegSyfoService (
    private val connection: DBConnection
) {

    private val log = LoggerFactory.getLogger(ProsesserStegSyfoService::class.java)
    private val dialogmeldingRepository = DialogmeldingRepository(connection)

    companion object {
        fun konstruer(connection: DBConnection): ProsesserStegSyfoService {
            return ProsesserStegSyfoService(connection)
        }
    }
    private val flyt = ProsesseringSyfoFlyt.Builder()
        .med(steg = StartLegeerklæringBestillingSteg, utfall = ProsesseringSyfoStatus.STARTET)
        .med(steg = JournalførBestillingSteg, utfall = ProsesseringSyfoStatus.JOURNALFØRT)
        .med(steg = BestillLegeerklæringSteg, utfall = ProsesseringSyfoStatus.SENDT_TIL_SYFO)
        .med(steg = FerdigLegeerklæringBestillingSteg, utfall = ProsesseringSyfoStatus.FERDIG)
        .build()

    fun prosesserBestilling(dialogmeldingUUID: UUID) {
        val bestillingStatus = dialogmeldingRepository.hentFlytStatus(dialogmeldingUUID)
        val stegene = flyt.fraStatus(bestillingStatus.flytStatus)

        if (stegene.isEmpty()) {
            log.warn("Forsøkte å prosessere bestilling uten flere steg og status ${bestillingStatus.flytStatus}.")
            return
        }

        stegene.forEach { steg ->
            val stegResultat = steg.konstruer(connection).utfør(SyfoSteg.Kontekst(dialogmeldingUUID))

            if (stegResultat == SyfoSteg.Resultat.STOPP) {
                throw RuntimeException("Stoppet i steg ${steg.javaClass.name}, $bestillingStatus")
            }

            dialogmeldingRepository.oppdaterFlytStatus(dialogmeldingUUID, flyt.utfall(steg))

            connection.markerSavepoint()
        }
    }
}