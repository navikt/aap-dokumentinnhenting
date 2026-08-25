package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.Azp
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.FellesDialogmeldingDto
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.InnkommendeUtgaaende
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

data class DialogmeldingIdParameter(@param:PathParam("dialogmeldingId") val dialogmeldingId: UUID)

data class DialogmeldingEksistererDto(val eksisterer: Boolean)

fun NormalOpenAPIRoute.dialogmeldingApi(
    dataSource: DataSource,
) {
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
                    sakPathParam = SakPathParam("saksnummer"),
                    applicationsOnly = true
                )
            ) { params ->
                val saksnummer = params.saksnummer
                val dialogmeldingerDtos = mutableListOf<FellesDialogmeldingDto>()

                val sendteDialogmeldinger = dataSource.transaction { connection ->
                    DialogmeldingRepository(connection).hentForSaksnummer(saksnummer)
                }

                sendteDialogmeldinger.forEach { dialogmelding ->
                    dialogmeldingerDtos.add(FellesDialogmeldingDto(
                        innkommendeUtgaaende = InnkommendeUtgaaende.UTGÅENDE,
                        meldingFraNavn = dialogmelding.behandlerNavn,
                        opprettetTidspunkt = dialogmelding.opprettet,
                        dokumentasjonsType = dialogmelding.dokumentasjonType.tilDto(),
                        tekst = dialogmelding.fritekst,
                        meldingStatus = dialogmelding.status?.mapLeveringStatus(),
                        journalpostId = dialogmelding.journalpostId
                    ))
                }

                val mottatteDialogmeldinger = dataSource.transaction { connection ->
                    MottattDialogmeldingRepository(connection).hentForSaksnummer(saksnummer)
                }

                mottatteDialogmeldinger.forEach { dialogmelding ->
                    dialogmeldingerDtos.add(FellesDialogmeldingDto(
                        innkommendeUtgaaende = InnkommendeUtgaaende.INNKOMMENDE,
                        meldingFraNavn = dialogmelding.navnHelsepersonell,
                        opprettetTidspunkt = dialogmelding.opprettetTid,
                        dokumentasjonsType = null,
                        tekst = dialogmelding.tekstNotatInnhold,
                        meldingStatus = null,
                        journalpostId = dialogmelding.journalpostId
                    ))
                }

                respond(dialogmeldingerDtos)
            }
        }
    }
}

data class HentDialogmeldingerForSakParams(
    @param:PathParam(description = "Saksnummer") val saksnummer: String,
)

