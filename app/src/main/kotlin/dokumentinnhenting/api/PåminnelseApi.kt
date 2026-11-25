package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestillingService
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import dokumentinnhenting.unleash.UnleashGateway
import dokumentinnhenting.util.DialogmeldingUUID
import dokumentinnhenting.util.Tags
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.PåminnelseDto
import no.nav.aap.dokumentinnhenting.kontrakt.PåminnelseInfoResponse
import no.nav.aap.dokumentinnhenting.kontrakt.PåminnelseStatusResponse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import java.util.UUID
import javax.sql.DataSource

fun NormalOpenAPIRoute.påminnelseApi(
    dataSource: DataSource,
    unleash: UnleashGateway
) {
    route("/dialogmelding/paaminnelse").tag(Tags.Påminnelse) {
        route("/send").authorizedPost<Unit, UUID, PåminnelseRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
            )
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection, unleash)
                service.validerBehandlingsreferanseForBestilling(req.dialogmeldingUuid, req.behandlingsreferanse)
                service.sendPåminnelseForBestilling(
                    DialogmeldingUUID(req.dialogmeldingUuid),
                    bruker()
                )
            }
            respond(response)
        }

        route("/avbryt-automatisk-paaminnelse").authorizedPost<Unit, HttpStatusCode, PåminnelseRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
            )
        ) { _, req ->
            dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection, unleash)
                service.validerBehandlingsreferanseForBestilling(req.dialogmeldingUuid, req.behandlingsreferanse)
                service.avbrytPåminnelseForBestilling(
                    DialogmeldingUUID(req.dialogmeldingUuid),
                    bruker()
                )
            }
            respond(HttpStatusCode.NoContent)
        }

        route("/gjenoppta-automatisk-paaminnelse").authorizedPost<Unit, HttpStatusCode, PåminnelseRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
            )
        ) { _, req ->
            dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection, unleash)
                service.validerBehandlingsreferanseForBestilling(req.dialogmeldingUuid, req.behandlingsreferanse)
                service.gjenopptaPåminnelseForBestilling(
                    DialogmeldingUUID(req.dialogmeldingUuid),
                    bruker()
                )
            }
            respond(HttpStatusCode.NoContent)
        }


        route("/{behandlingsreferanse}/hent").authorizedGet<HentPåminnelserForBehandlingPathParams, PåminnelseInfoResponse>(
            AuthorizationMachineToMachineConfig(
                authorizedRoles = listOf("paaminnelse-info-api")
            )
        ) { req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection, unleash)

                val påminnelser = service.hentPåminnelserForBehandling(BehandlingReferanse(req.behandlingsreferanse))

                PåminnelseInfoResponse(
                    påminnelser = påminnelser.map {
                        PåminnelseDto(
                            status = mapPåminnelseStatusTilResponse(it.status),
                            påminnelseDato = it.påminnelseDato,
                            opprettetTidspunkt = it.opprettetTidspunkt
                        )
                    }
                )
            }
            respond(response)
        }
    }
}

data class HentPåminnelserForBehandlingPathParams(
    @param:PathParam(description = "Behandlingsreferanse") val behandlingsreferanse: UUID,
)

private fun mapPåminnelseStatusTilResponse(status: PåminnelseStatus): PåminnelseStatusResponse {
    return when (status) {
        PåminnelseStatus.PLANLAGT -> PåminnelseStatusResponse.PLANLAGT
        PåminnelseStatus.SENDT -> PåminnelseStatusResponse.SENDT
        PåminnelseStatus.AVBRUTT -> PåminnelseStatusResponse.AVBRUTT
    }
}