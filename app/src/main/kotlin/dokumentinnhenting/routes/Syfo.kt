package dokumentinnhenting.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.syfo.bestilling.*
import dokumentinnhenting.integrasjoner.syfo.oppslag.BehandlerOppslagResponse
import dokumentinnhenting.integrasjoner.syfo.oppslag.FritekstRequest
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import dokumentinnhenting.integrasjoner.syfo.status.HentDialogmeldingStatusDTO
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.BestillingCache
import io.ktor.http.*
import no.nav.aap.komponenter.dbconnect.transaction
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.syfo(dataSource: DataSource
) {
    route("/syfo") {
        route("/dialogmeldingbestilling").post<Unit, UUID, BehandlingsflytToDokumentInnhentingBestillingDTO> { _, req ->
            if (BestillingCache.contains(req.saksnummer)) {
                respondWithStatus(HttpStatusCode.TooManyRequests)
            }

            val response = dataSource.transaction { connection ->
                BestillingCache.add(req.saksnummer)
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.dialogmeldingBestilling(req)
            }
            respond (response)
        }

        route("/purring").post<Unit, UUID, LegeerklæringPurringDTO> { _, req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.dialogmeldingPurring(req)
            }
            respond (response)
        }

        route("/status/{saksnummer}").get<HentDialogmeldingStatusDTO, List<DialogmeldingStatusTilBehandslingsflytDTO>> { req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.hentBySaksnummer(req.saksnummer)
            }
            respond(response)
        }

        route("/behandleroppslag/search").post<Unit, List<BehandlerOppslagResponse>, FritekstRequest> { _, req ->
            val behandlere = SyfoGateway().frisøkBehandlerOppslag(req.fritekst)
            respond(behandlere)
        }

        route("/brevpreview").post<Unit, BrevPreviewResponse, BrevGenereringRequest> { _, req ->
            val response = dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val tidligereBestilling = req.tidligeBestillingReferanse?.let { dialogmeldingRepository.hentBestillingEldreEnn14Dager(it) }

                val brevPreviewResponse = BrevPreviewResponse(genererBrev(
                    BrevGenerering(
                        req.personNavn, req.personIdent, req.dialogmeldingTekst, req.veilederNavn, req.dokumentasjonType, tidligereBestilling?.opprettet
                    )
                ))
                brevPreviewResponse
            }
            respond(response)
        }
    }
}
