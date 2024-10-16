package integrasjonportal.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import integrasjonportal.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestilling
import integrasjonportal.integrasjoner.syfo.bestilling.BehandlingsflytToDialogmeldingDTO

fun NormalOpenAPIRoute.syfo(service: BehandlerDialogmeldingBestilling
) {
    route("/syfo") {
        route("/dialogmeldingbestilling") {
            post<Unit, Unit, BehandlingsflytToDialogmeldingDTO> { _, req ->
                service.dialogmeldingBestilling(req)
            }
        }
    }

    route("/syfo") {
        route("/status") {
            // TODO: Implement get status
        }
    }
}
