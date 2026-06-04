package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestillingService
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingBrevGeneratorService
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.oppslag.FritekstRequest
import dokumentinnhenting.integrasjoner.syfo.oppslag.HentFastlegeDtoSaksreferanse
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.BestillingCache
import io.ktor.http.HttpStatusCode
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.runBlocking
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlerDto
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingForhåndsvisningDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.FastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import no.nav.aap.dokumentinnhenting.kontrakt.MarkerBestillingSomMottattDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.Rolle
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.syfoApi(
    dataSource: DataSource,
    brevGateway: BrevGateway,
    syfoGateway: SyfoGateway
) {
    val syfoApiRolle = "syfo-api"
    val brevGeneratorService = DialogmeldingBrevGeneratorService(brevGateway)
    route("/syfo") {
        route("/dialogmeldingbestilling").authorizedPost<Unit, UUID, BehandlingsflytToDokumentInnhentingBestillingDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            if (BestillingCache.contains(req.saksnummer)) {
                respondWithStatus(HttpStatusCode.TooManyRequests)
                return@authorizedPost
            }

            if (req.behandlerHprNr.length < 7 || req.behandlerHprNr.length > 9) {
                respondWithStatus(HttpStatusCode.BadRequest)
                return@authorizedPost
            }

            val response = dataSource.transaction { connection ->
                BestillingCache.add(req.saksnummer)
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.dialogmeldingBestilling(req)
            }
            respond(response)
        }

        route("/purring").authorizedPost<Unit, UUID, LegeerklæringPurringDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.dialogmeldingPurring(req)
            }
            respond(response)
        }

        route("/status/markerbestillingmottatt").authorizedPost<Unit, DialogmeldingStatusTilBehandslingsflytDto, MarkerBestillingSomMottattDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.oppdaterDialogmeldingStatusMedMottatt(req.dialogmeldingUuid)
                val record = requireNotNull(repository.hentByDialogId(req.dialogmeldingUuid))
                record.tilDto()
            }
            respond(response)
        }

        route("/status/{saksnummer}").authorizedGet<SaksnummerParameter, List<DialogmeldingStatusTilBehandslingsflytDto>>(
            AuthorizationParamPathConfig(
                applicationRole = syfoApiRolle,
                applicationsOnly = true,
                sakPathParam = SakPathParam("saksnummer")
            )
        ) { req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.hentBySaksnummer(req.saksnummer)
                    .map(DialogmeldingFullRecord::tilDto)
            }
            respond(response)
        }

        route("/behandleroppslag/fastlege").authorizedPost<Unit, FastlegeDto, HentFastlegeDtoSaksreferanse>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                påkrevdRolle = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING, Rolle.SAKSBEHANDLER_NASJONAL),
                applicationRole = syfoApiRolle,
                applicationsOnly = false
            )
        ) { _, req ->
            val behandlere = syfoGateway.behandlere(req.personIdent, token())
            val fastlege = behandlere.find { it.type == "FASTLEGE" }
            respond(FastlegeDto(fastlege?.tilDto()))
        }

        route("/behandleroppslag/search").authorizedPost<Unit, List<BehandlerDto>, FritekstRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationRole = syfoApiRolle,
                applicationsOnly = false
            )
        ) { _, req ->
            val behandlere = syfoGateway.frisøkBehandlerOppslag(req.fritekst, token())

            respond(behandlere.map { it.tilDto() })
        }

        route("/brevpreview").authorizedPost<Unit, DialogmeldingForhåndsvisningDto, ForhåndsvisDialogmeldingDto>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val tidligereBestilling =
                    req.tidligereBestillingReferanse?.let { dialogmeldingRepository.hentBestillingEldreEnn14Dager(it) }

                val dialogmelding = runBlocking {
                    brevGeneratorService.genererMedSignatur(
                        personNavn = req.personNavn,
                        personIdent = req.personIdent,
                        dialogmeldingTekst = req.dialogmeldingTekst,
                        dokumentasjonType = req.dokumentasjonType.fraDto(),
                        tidligereBestillingDato = tidligereBestilling?.opprettet,
                        bestillerNavIdent = req.bestillerNavIdent,
                    )
                }
                DialogmeldingForhåndsvisningDto(dialogmelding)
            }
            respond(response)
        }
    }
}
