package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.oppslag.BehandlerOppslagResponse
import dokumentinnhenting.integrasjoner.syfo.oppslag.FritekstRequest
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.driftApi(dataSource: DataSource) {
    route("/drift/api") {
        route("/syfo/behandleroppslag/search").authorizedPost<Unit, List<BehandlerOppslagResponse>, FritekstRequest>(
            AuthorizationBodyPathConfig(operasjon = Operasjon.DRIFTE, applicationsOnly = false)
        ) { _, req ->
            val behandlere = SyfoGateway().frisøkBehandlerOppslag(req.fritekst, token())

            respond(behandlere)
        }

        route("/sak/{saksnummer}/dialogmelding").authorizedPost<SaksnummerParameter, List<DialogmeldingDriftinfoDTO>, Unit>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.DRIFTE,
            ),
        ) { params, _ ->
            val response = dataSource.transaction { connection ->
                DialogmeldingRepository(connection)
                    .hentBySaksnummer(params.saksnummer)
                    .map {
                        DialogmeldingDriftinfoDTO(
                            bestillerNavIdent = it.bestillerNavIdent,
                            dialogmeldingUuid = it.dialogmeldingUuid,
                            behandlerRef = it.behandlerRef,
                            behandlerHprNr = it.behandlerHprNr,
                            dokumentasjonType = it.dokumentasjonType,
                            status = it.status,
                            flytStatus = it.flytStatus,
                            statusTekst = it.statusTekst,
                            behandlingsReferanse = it.behandlingsReferanse,
                            opprettet = it.opprettet,
                            tidligereBestillingReferanse = it.tidligereBestillingReferanse,
                            journalpostId = it.journalpostId,
                            dokumentId = it.dokumentId
                        )
                    }
            }

            krevDtoErUtenFødselsnummer(response)
            respond(response)
        }
    }
}

private fun krevDtoErUtenFødselsnummer(dto: Any) {
    if (Regex("""(?<!\w)\d{11}(?!\w)""").containsMatchIn(DefaultJsonMapper.toJson(dto))) {
        throw IkkeTillattException("DTO-en inneholder (potensielt) sensitive personopplysninger!")
    }
}

data class SaksnummerParameter(@param:PathParam("saksnummer") val saksnummer: String)

data class DialogmeldingDriftinfoDTO(
    val bestillerNavIdent: String,
    val dialogmeldingUuid: UUID,
    val behandlerRef: String,
    val behandlerHprNr: String,
    val dokumentasjonType: DokumentasjonType,
    val status: MeldingStatusType?,
    val flytStatus: ProsesseringSyfoStatus?,
    val statusTekst: String?,
    val behandlingsReferanse: UUID,
    val opprettet: LocalDateTime,
    val tidligereBestillingReferanse: UUID?,
    val journalpostId: String?,
    val dokumentId: String?,
)
