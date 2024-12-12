package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvvistLegeerklæringId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.util.*

fun NormalOpenAPIRoute.testApi () {
    route("/test") {
        route("/avvist").post<Unit, String, TaAvVentRequest> { _, req ->

            val behandlingsflytClient = BehandlingsflytClient()
            behandlingsflytClient.taSakAvVent(
                MottattHendelseDto(
                    Saksnummer(req.saksnummer),
                    InnsendingType.LEGEERKLÆRING_AVVIST,
                    Kanal.DIGITAL,
                    InnsendingReferanse(AvvistLegeerklæringId(UUID.randomUUID())),
                    AvvistLegeerklæringId(req.saksnummer)
                )
            )
            respond("", HttpStatusCode.OK)
        }
    }
}

data class TaAvVentRequest(
    val saksnummer: String
)