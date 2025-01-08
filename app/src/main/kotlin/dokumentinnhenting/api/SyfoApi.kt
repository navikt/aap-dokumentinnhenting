package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
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
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import tilgang.Operasjon
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.syfoApi(dataSource: DataSource) {
    val behandlingsflytAzp = requiredConfigForKey("integrasjon.behandlingsflyt.azp")
    val saksbehandlingAzp = requiredConfigForKey("integrasjon.saksbehandling.azp")
    route("/syfo") {
        route("/dialogmeldingbestilling").authorizedPost<Unit, UUID, BehandlingsflytToDokumentInnhentingBestillingDTO>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                approvedApplications = setOf(behandlingsflytAzp),
                applicationsOnly = true)
            ) { _, req ->
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

        route("/purring").authorizedPost<Unit, UUID, LegeerklæringPurringDTO>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                approvedApplications = setOf(behandlingsflytAzp),
                applicationsOnly = true)
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.dialogmeldingPurring(req)
            }
            respond (response)
        }

        route("/status/{saksnummer}").authorizedGet<HentDialogmeldingStatusDTO, List<DialogmeldingStatusTilBehandslingsflytDTO>>(
            AuthorizationParamPathConfig(
                approvedApplications = setOf(behandlingsflytAzp),
                applicationsOnly = true)
        ) { req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.hentBySaksnummer(req.saksnummer)
            }
            respond(response)
        }

        route("/behandleroppslag/search").authorizedPost<Unit, List<BehandlerOppslagResponse>, FritekstRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                approvedApplications = setOf(saksbehandlingAzp),
                applicationsOnly = true
            )
        ) { _, req ->
            val behandlere = SyfoGateway().frisøkBehandlerOppslag(req.fritekst)
            respond(behandlere)
        }

        route("/brevpreview").authorizedPost<Unit, BrevPreviewResponse, BrevGenereringRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                approvedApplications = setOf(behandlingsflytAzp),
                applicationsOnly = true)
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val tidligereBestilling = req.tidligereBestillingReferanse?.let { dialogmeldingRepository.hentBestillingEldreEnn14Dager(it) }

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
