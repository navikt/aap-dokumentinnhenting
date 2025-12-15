package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.behandlingsflyt.VarselOmBrevbestillingDto
import dokumentinnhenting.integrasjoner.brev.BrevGateway
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
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

// Dette API'et er kun for testmiljø for å kunne teste hele verdikjeden
fun NormalOpenAPIRoute.testApi(dataSource: DataSource) {
    val testApiRolle = "test-api"
    route("/test") {
        route("/avvist").authorizedPost<Unit, String, TaAvVentRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = testApiRolle,
                applicationsOnly = true)
        ) { _, req ->

            val behandlingsflytGateway = BehandlingsflytGateway
            val avvistLegeerklæringId = UUID.randomUUID()
            behandlingsflytGateway.taSakAvVent(
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

        route("/ekspeder").authorizedPost<Unit, String, TestRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = testApiRolle,
                applicationsOnly = true)
        ) { _, req ->
            dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val fullRecord = requireNotNull(dialogmeldingRepository.hentByDialogId(req.dialogid))

                BrevGateway().ekspederBestilling(
                    BrevGateway.EkspederBestillingRequest(
                        fullRecord.journalpostId!!, fullRecord.dokumentId!!
                    ))
            }
            respond("", HttpStatusCode.OK)
        }

        route("/varselbrev").authorizedPost<Unit, String, TestRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = testApiRolle,
                applicationsOnly = true)
        ) { _, req ->
            dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val fullRecord = requireNotNull(dialogmeldingRepository.hentByDialogId(req.dialogid))

                BehandlingsflytGateway.sendVarslingsbrev(
                    VarselOmBrevbestillingDto(
                        BehandlingReferanse(
                            fullRecord.behandlingsReferanse
                        ),
                        fullRecord.dialogmeldingUuid,
                        Vedlegg(
                            fullRecord.journalpostId!!,
                            fullRecord.dokumentId!!
                        )
                    )
                )
            }
            respond("", HttpStatusCode.OK)
        }
    }

}
data class TaAvVentRequest(
    val saksnummer: String
)

data class TestRequest(
    val dialogid: UUID
)