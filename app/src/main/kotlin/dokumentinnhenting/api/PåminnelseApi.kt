package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestillingService
import io.ktor.http.HttpStatusCode
import no.nav.aap.dokumentinnhenting.kontrakt.PåminnelseDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.util.UUID
import javax.sql.DataSource

fun NormalOpenAPIRoute.påminnelseApi(
    dataSource: DataSource
) {
    val paaminnelseApiRolle = "paaminnelse-api"
    route("/dialogmelding/paaminnelse") {
        route("/send").authorizedPost<Unit, UUID, PåminnelseDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = paaminnelseApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.sendPåminnelseForBestilling(req.dialogmeldingUuid)
            }
            respond(response)
        }


        route("/avbryt-automatisk-paaminnelse").authorizedPost<Unit, HttpStatusCode, PåminnelseDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = paaminnelseApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.avbrytPåminnelseForBestilling(req.dialogmeldingUuid)
            }
            respond(HttpStatusCode.NoContent)
        }

        route("/gjenoppta-automatisk-paaminnelse").authorizedPost<Unit, HttpStatusCode, PåminnelseDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = paaminnelseApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.gjenopptaPåminnelseForBestilling(req.dialogmeldingUuid)
            }
            respond(HttpStatusCode.NoContent)
        }
    }
}