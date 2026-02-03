package dokumentinnhenting.util.motor.syfo.syfosteg

import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.repositories.DialogmeldingRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

class JournalførBestillingSteg(
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val brevGateway: BrevGateway
): SyfoSteg.Utfører {
    private val log = LoggerFactory.getLogger(StartLegeerklæringBestillingSteg::class.java)

    override fun utfør(kontekst: SyfoSteg.Kontekst): SyfoSteg.Resultat {
        log.info("JournalførBestillingSteg")
        return journalførBestilling(kontekst.referanse)
    }

    companion object : SyfoSteg {
        override fun konstruer(connection: DBConnection): SyfoSteg.Utfører {
            return JournalførBestillingSteg(
                DialogmeldingRepository(connection),
                BrevGateway()
            )
        }
    }

    //TODO: Her må vi ha noe logikk på ferdigstilling?
    fun journalførBestilling(dialogmeldingUuid: UUID): SyfoSteg.Resultat {
        val bestilling = requireNotNull(dialogmeldingRepository.hentByDialogId(dialogmeldingUuid))
        val tidligereTilhørendeBestillingsdato = bestilling.tidligereBestillingReferanse?.let { dialogmeldingRepository.hentBestillingEldreEnn14Dager(it)?.opprettet }

        try {
            val journalpostResponse = runBlocking {
                brevGateway.journalførBestilling(bestilling, tidligereTilhørendeBestillingsdato)
            }
            val dokumentId = journalpostResponse.dokumenter[0]

            if (!journalpostResponse.journalpostFerdigstilt) {
                log.warn("Greide ikke ferdigstille journal med id ${journalpostResponse.journalpostId}")
            }

            dialogmeldingRepository.leggTilJournalpostPåBestilling(dialogmeldingUuid, requireNotNull(journalpostResponse.journalpostId), dokumentId)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.RequestTimeout) {
                log.warn("Timeout ved journalføring av dokument $dialogmeldingUuid", e)
                return SyfoSteg.Resultat.STOPP
            } else {
                log.error("Uhåndtert feil ved journalføring av dokument $dialogmeldingUuid", e)
                return SyfoSteg.Resultat.STOPP
            }
        } catch (e: Exception) {
            log.error("Feilet ved journalføring av dokument $dialogmeldingUuid", e)
            return SyfoSteg.Resultat.STOPP
        }

        return SyfoSteg.Resultat.FULLFØRT
    }
}