package dokumentinnhenting.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.Azp
import dokumentinnhenting.integrasjoner.saf.SafGateway
import dokumentinnhenting.integrasjoner.saf.Saksnummer
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.FellesDialogmeldingDto
import dokumentinnhenting.integrasjoner.syfo.dialogmeldinger.InnkommendeUtgaaende
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedGet
import org.slf4j.LoggerFactory

data class DialogmeldingIdParameter(@param:PathParam("dialogmeldingId") val dialogmeldingId: UUID)

data class DialogmeldingEksistererDto(val eksisterer: Boolean)

fun NormalOpenAPIRoute.dialogmeldingApi(
    dataSource: DataSource,
) {
    val logger = LoggerFactory.getLogger("DialogmeldingApi")

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


        // TODO: Map til nytt format med nødvendig data
        // TODO: Flytte denne til DialogmeldingApi?
        route("/{saksnummer}/dialogmeldinger") {
            authorizedGet<HentDialogmeldingOversiktFagsakParams, List<FellesDialogmeldingDto>>(
                AuthorizationMachineToMachineConfig(
                    authorizedAzps = listOf(Azp.ApiIntern)
                )
            ) { params ->
                val dialogmeldingerDtos = mutableListOf<FellesDialogmeldingDto>()

                val sendteDialogmeldinger = dataSource.transaction { connection ->
                    DialogmeldingRepository(connection).hentBySaksnummer(params.saksnummer)
                }

                sendteDialogmeldinger.forEach { dialogmelding ->
                    dialogmeldingerDtos.add(FellesDialogmeldingDto(
                        innkommendeUtgaaende = InnkommendeUtgaaende.UTGÅENDE,
                        meldingFraNavn = dialogmelding.behandlerNavn,
                        opprettetTidspunkt = dialogmelding.opprettet,
                        dokumentasjonsType = dialogmelding.dokumentasjonType.tilDto(),
                        tekst = dialogmelding.fritekst,
                        meldingStatus = dialogmelding.status?.mapLeveringStatus(),
                        journalpostId = dialogmelding.journalpostId,
                        dokumentIdListe = mutableListOf()
                    ))
                }

                val mottatteDialogmeldinger = dataSource.transaction { connection ->
                    MottattDialogmeldingRepository(connection).hentBySaksnummer(params.saksnummer)
                }

                mottatteDialogmeldinger.forEach { dialogmelding ->
                    dialogmeldingerDtos.add(FellesDialogmeldingDto(
                        innkommendeUtgaaende = InnkommendeUtgaaende.INNKOMMENDE,
                        meldingFraNavn = dialogmelding.navnHelsepersonell,
                        opprettetTidspunkt = dialogmelding.opprettetTid,
                        dokumentasjonsType = null,
                        tekst = dialogmelding.dn,
                        // TODO: TekstNotatInnhold er navnet på lenka, ha som eget felt eller funker tittel?
                        meldingStatus = null,
                        journalpostId = dialogmelding.journalpostId,
                        dokumentIdListe = mutableListOf()
                    ))
                }

                if (dialogmeldingerDtos.isNotEmpty()) {
                    val dokumenterForSak = SafGateway.hentDokumenterForSak(Saksnummer(params.saksnummer), token())
                    dokumenterForSak.map { dokument ->
                        val dialogmelding =
                            dialogmeldingerDtos.firstOrNull { dto -> dto.journalpostId == dokument.journalpostId }
                        if (dialogmelding != null) {
                            dialogmelding.dokumentIdListe.addAll(dokument.dokumenter)
                        }
                    }
                }

                respond(dialogmeldingerDtos)
            }
        }
    }
}

data class HentDialogmeldingOversiktFagsakParams(
    @param:PathParam(description = "Saksnummer") val saksnummer: String,
)

