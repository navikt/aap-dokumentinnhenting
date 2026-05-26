package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.Azp
import dokumentinnhenting.repositories.DialogmeldingRepository
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedGet
import org.slf4j.LoggerFactory

data class DialogmeldingIdParameter(@param:PathParam("dialogmeldingId") val dialogmeldingId: UUID)

data class DialogmeldingEksistererDto(val eksisterer: Boolean)

fun NormalOpenAPIRoute.dialogmeldingApi(
    dataSource: DataSource,
) {
    val logger = LoggerFactory.getLogger("DialogmeldingApi")

    route("/dialogmelding") {
        route("/{dialogmeldingId}/eksisterer") {
            authorizedGet<DialogmeldingIdParameter, DialogmeldingEksistererDto>(
                AuthorizationMachineToMachineConfig(
                    authorizedAzps = listOf(Azp.ApiIntern)
                )
            ) { params ->
                val dialogmeldingEksisterer = dataSource.transaction { connection ->
                    DialogmeldingRepository(connection)
                        .eksisterer(params.dialogmeldingId)
                }

                logger.info("Dialogmelding med ID ${params.dialogmeldingId} eksisterer: $dialogmeldingEksisterer")

                respond(DialogmeldingEksistererDto(dialogmeldingEksisterer))
            }
        }
    }
}
