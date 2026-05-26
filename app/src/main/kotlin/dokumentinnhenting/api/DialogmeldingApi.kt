package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.repositories.DialogmeldingRepository
import io.ktor.http.HttpStatusCode
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.transaction

data class DialogmeldingIdParameter(@param:PathParam("dialogmeldingId") val dialogmeldingId: UUID)

data class DialogmeldingEksistererDto(val eksisterer: Boolean)

fun NormalOpenAPIRoute.dialogmeldingApi(
    dataSource: DataSource,
) {
    route("/dialogmelding") {
        route("/{dialogmeldingId}/eksisterer").get<DialogmeldingIdParameter, DialogmeldingEksistererDto> { params ->
            val dialogmeldingEksisterer = dataSource.transaction { connection ->
                DialogmeldingRepository(connection)
                    .eksisterer(params.dialogmeldingId)
            }

            if (dialogmeldingEksisterer) {
                respond(DialogmeldingEksistererDto(true), HttpStatusCode.OK)
            } else {
                respond(DialogmeldingEksistererDto(false), HttpStatusCode.NoContent)
            }
        }
    }
}