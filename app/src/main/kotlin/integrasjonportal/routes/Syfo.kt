package integrasjonportal.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import integrasjonportal.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestilling
import integrasjonportal.integrasjoner.syfo.bestilling.BehandlingsflytToDialogmeldingDTO
import integrasjonportal.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import integrasjonportal.integrasjoner.syfo.status.HentDialogmeldingStatusDto
import integrasjonportal.repositories.DialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

fun NormalOpenAPIRoute.syfo(service: BehandlerDialogmeldingBestilling, dataSource: DataSource
) {
    route("/syfo") {
        route("/dialogmeldingbestilling").post<Unit, Unit, BehandlingsflytToDialogmeldingDTO> { _, req ->
            service.dialogmeldingBestilling(req)
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
