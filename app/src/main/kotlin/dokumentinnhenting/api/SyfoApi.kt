package dokumentinnhenting.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import dokumentinnhenting.integrasjoner.brev.BrevGateway
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlerDialogmeldingBestillingService
import dokumentinnhenting.integrasjoner.syfo.bestilling.BehandlingsflytToDokumentInnhentingBestillingDTO
import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevGenerering
import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevGenereringRequest
import dokumentinnhenting.integrasjoner.syfo.bestilling.BrevPreviewResponse
import dokumentinnhenting.integrasjoner.syfo.bestilling.LegeerklæringPurringDTO
import dokumentinnhenting.integrasjoner.syfo.bestilling.MarkerBestillingSomMottattDTO
import dokumentinnhenting.integrasjoner.syfo.bestilling.genererBrev
import dokumentinnhenting.integrasjoner.syfo.oppslag.BehandlerOppslagResponse
import dokumentinnhenting.integrasjoner.syfo.oppslag.FritekstRequest
import dokumentinnhenting.integrasjoner.syfo.oppslag.SyfoGateway
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import dokumentinnhenting.integrasjoner.syfo.status.HentDialogmeldingStatusDTO
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.util.BestillingCache
import io.ktor.http.HttpStatusCode
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost


fun NormalOpenAPIRoute.syfoApi(dataSource: DataSource) {
    val syfoApiRolle = "syfo-api"
    route("/syfo") {
        route("/dialogmeldingbestilling").authorizedPost<Unit, UUID, BehandlingsflytToDokumentInnhentingBestillingDTO>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true)
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
            respond (response)
        }

        route("/purring").authorizedPost<Unit, UUID, LegeerklæringPurringDTO>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true)
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val service = BehandlerDialogmeldingBestillingService.konstruer(connection)
                service.dialogmeldingPurring(req)
            }
            respond (response)
        }

        route("/status/markerbestillingmottatt").authorizedPost<Unit, DialogmeldingStatusTilBehandslingsflytDTO, MarkerBestillingSomMottattDTO>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true)
        ) { _, req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.oppdaterDialogmeldingStatusMedMottatt(req.dialogmeldingUuid)
                val record = requireNotNull(repository.hentByDialogId(req.dialogmeldingUuid))

                DialogmeldingStatusTilBehandslingsflytDTO(
                    dialogmeldingUuid = req.dialogmeldingUuid,
                    status = record.status,
                    statusTekst = record.statusTekst,
                    behandlerRef = record.behandlerRef,
                    behandlerNavn = record.behandlerNavn,
                    personId = record.personIdent,
                    saksnummer = record.saksnummer,
                    opprettet = record.opprettet,
                    behandlingsReferanse = record.behandlingsReferanse,
                    fritekst = record.fritekst
                )
            }
            respond(response)
        }

        route("/status/{saksnummer}").authorizedGet<HentDialogmeldingStatusDTO, List<DialogmeldingStatusTilBehandslingsflytDTO>>(
            AuthorizationParamPathConfig(
                applicationRole = syfoApiRolle,
                applicationsOnly = true,
                sakPathParam = SakPathParam("saksnummer")
            )
        ) { req ->
            val response = dataSource.transaction { connection ->
                val repository = DialogmeldingRepository(connection)
                repository.hentBySaksnummer(req.saksnummer)
            }
            respond(response)
        }

        route("/behandleroppslag/search").authorizedPost<Unit, List<BehandlerOppslagResponse>, FritekstRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationRole = syfoApiRolle,
                applicationsOnly = false
            )
        ) { _, req ->
            val behandlere = SyfoGateway().frisøkBehandlerOppslag(req.fritekst, token())

            respond(behandlere)
        }

        route("/brevpreview").authorizedPost<Unit, BrevPreviewResponse, BrevGenereringRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SAKSBEHANDLE,
                applicationRole = syfoApiRolle,
                applicationsOnly = true
            )
        ) { _, req ->
            val signatur = BrevGateway().hentSignaturForhåndsvisning(req.personIdent, req.bestillerNavIdent)
            val response = dataSource.transaction { connection ->
                val dialogmeldingRepository = DialogmeldingRepository(connection)
                val tidligereBestilling =
                    req.tidligereBestillingReferanse?.let { dialogmeldingRepository.hentBestillingEldreEnn14Dager(it) }

                val brevtekst = genererBrev(
                    BrevGenerering(
                        personNavn = req.personNavn,
                        personIdent = req.personIdent,
                        dialogmeldingTekst = req.dialogmeldingTekst,
                        dokumentasjonType = req.dokumentasjonType,
                        tidligereBestillingDato = tidligereBestilling?.opprettet,
                    )
                )
                val signaturtekst = if (signatur != null) {
                    """\n\nMed vennlig hilsen\n${signatur.navn}\n${signatur.enhet}"""
                } else {
                    ""
                }

                BrevPreviewResponse(brevtekst + signaturtekst)
            }
            respond(response)
        }
    }
}
