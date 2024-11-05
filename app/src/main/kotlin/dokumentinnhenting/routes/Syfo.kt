package dokumentinnhenting.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestilling
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlingsflytToDialogmeldingDTO
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import dokumentinnhenting.integrasjoner.syfo.status.HentDialogmeldingStatusDto
import dokumentinnhenting.repositories.DialogmeldingRepository
import io.ktor.events.*
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

fun NormalOpenAPIRoute.syfo(dataSource: DataSource, monitor: Events
) {
    route("/syfo") {
        route("/dialogmeldingbestilling").post<Unit, String, BehandlingsflytToDialogmeldingDTO> { _, req ->
            val service = BehandlerDialogmeldingBestilling(monitor, dataSource)
            respond (service.dialogmeldingBestilling(req))
        }

        route("/status/{saksnummer}").get<HentDialogmeldingStatusDto, List<DialogmeldingStatusTilBehandslingsflytDTO>> { req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.hentBySakId(req.saksnummer)
            }
            respond(response)
        }
    }
}
