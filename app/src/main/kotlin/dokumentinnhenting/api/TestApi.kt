package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytClient
import dokumentinnhenting.integrasjoner.behandlingsflyt.VarselOmBrevbestillingDto
import dokumentinnhenting.integrasjoner.brev.BrevClient
import dokumentinnhenting.repositories.DialogmeldingRepository
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvvistLegeerklæringId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.testApi(dataSource: DataSource) {
    route("/test") {
        route("/avvist").post<Unit, String, TaAvVentRequest> { _, req ->

            val behandlingsflytClient = BehandlingsflytClient()
            val avvistLegeerklæringId = UUID.randomUUID()
            behandlingsflytClient.taSakAvVent(
                Innsending(
                    saksnummer = Saksnummer(req.saksnummer),
                    referanse = InnsendingReferanse(AvvistLegeerklæringId(avvistLegeerklæringId)),
                    type = InnsendingType.LEGEERKLÆRING_AVVIST,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now(),
                    melding = null
                )
            )
            respond("", HttpStatusCode.OK)
        }

        route("/ekspeder").post<Unit, String, TestRequest> { _, req ->
            dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val fullRecord = dialogmeldingRepository.hentByDialogId(req.dialogid)

                val brevClient = BrevClient()
                brevClient.ekspederBestilling(
                    BrevClient.EkspederBestillingRequest(
                        req.toString(), fullRecord!!.dokumentId!!
                    ))
            }
            respond("", HttpStatusCode.OK)
        }

        route("/varselbrev").post<Unit, UUID, TestRequest> { _, req ->
            val response = dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val fullRecord = requireNotNull(dialogmeldingRepository.hentByDialogId(req.dialogid))

                val behandlingsflytClient = BehandlingsflytClient()
                val varsling = behandlingsflytClient.sendVarslingsbrev(
                    VarselOmBrevbestillingDto(
                        BehandlingReferanse(
                            fullRecord.behandlingsReferanse
                        ),
                        Vedlegg(
                            fullRecord.journalpostId!!,
                            fullRecord.dokumentId!!
                        )
                    )
                )
                varsling
            }
            respond(response, HttpStatusCode.OK)
        }
    }

}
data class TaAvVentRequest(
    val saksnummer: String
)

data class TestRequest(
    val dialogid: UUID
)