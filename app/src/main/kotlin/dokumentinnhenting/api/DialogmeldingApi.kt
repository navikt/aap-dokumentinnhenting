package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.Azp
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingUthentingService
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.authorizedGet
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

data class DialogmeldingIdParameter(@param:PathParam("dialogmeldingId") val dialogmeldingId: UUID)

data class DialogmeldingEksistererDto(val eksisterer: Boolean)

fun NormalOpenAPIRoute.dialogmeldingApi(
    dataSource: DataSource,
) {
    val dialogmeldingUthentingService = DialogmeldingUthentingService(dataSource)

    val logger = LoggerFactory.getLogger("DialogmeldingApi")
    val dialogmeldingApiRolle = "dialogmelding-api"

    route("/dialogmelding") {
        route("/{dialogmeldingId}/eksisterer") {
            authorizedGet<DialogmeldingIdParameter, DialogmeldingEksistererDto>(
                AuthorizationMachineToMachineConfig(
                    authorizedAzps = listOf(Azp.ApiIntern)
                )
            ) { params ->
                val dialogmeldingEksisterer = dataSource.transaction { connection ->
                    val eksistererUtsendtDialogmelding =
                        DialogmeldingRepository(connection).eksisterer(params.dialogmeldingId)

                    val eksistererMottattDialogmelding by lazy {
                        MottattDialogmeldingRepository(connection).eksisterer(params.dialogmeldingId)
                    }

                    eksistererUtsendtDialogmelding || eksistererMottattDialogmelding
                }

                logger.info("Dialogmelding med ID ${params.dialogmeldingId} eksisterer: $dialogmeldingEksisterer")

                respond(DialogmeldingEksistererDto(dialogmeldingEksisterer))
            }
        }

        route("/{saksnummer}/dialogmeldinger") {
            authorizedGet<HentDialogmeldingerForSakParams, List<FellesDialogmeldingDto>>(
                AuthorizationParamPathConfig(
                    applicationRole = dialogmeldingApiRolle,
                    applicationsOnly = true
                )
            ) { params ->
                respond(dialogmeldingUthentingService.hentFellesDialogmeldingerForSak(params.saksnummer))
            }
        }
    }
}

data class HentDialogmeldingerForSakParams(
    @param:PathParam(description = "Saksnummer") val saksnummer: String,
)

