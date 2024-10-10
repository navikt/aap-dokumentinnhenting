package tilgang

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon as PostmottakDefinisjon
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.saf.graphql.SafJournalpost
import org.slf4j.LoggerFactory
import tilgang.integrasjoner.behandlingsflyt.BehandlingsflytClient
import tilgang.integrasjoner.behandlingsflyt.IdenterRespons
import tilgang.regler.RegelInput
import tilgang.regler.RegelService

class TilgangService(
    private val safClient: SafGraphqlClient,
    private val behandlingsflytClient: BehandlingsflytClient,
    private val regelService: RegelService
) {
    private val log = LoggerFactory.getLogger(TilgangService::class.java)

    suspend fun harTilgangTilSak(
        ansattIdent: String,
        req: SakTilgangRequest,
        roller: List<Rolle>,
        token: String,
        callId: String
    ): Boolean {
        val identer = behandlingsflytClient.hentIdenterForSak(token, req.saksnummer)
        val regelInput = RegelInput(
            callId = callId,
            ansattIdent = ansattIdent,
            currentToken = token,
            roller = roller,
            søkerIdenter = identer,
            avklaringsbehovFraBehandlingsflyt = null,
            avklaringsbehovFraPostmottak = null,
            operasjon = req.operasjon
        )
        return regelService.vurderTilgang(regelInput)
    }

    suspend fun harTilgangTilBehandling(
        ansattIdent: String,
        req: BehandlingTilgangRequest,
        roller: List<Rolle>,
        token: String,
        callId: String
    ): Boolean {
        val identer = behandlingsflytClient.hentIdenterForBehandling(token, req.behandlingsreferanse)
        val avklaringsbehov =
            if (req.avklaringsbehovKode != null) Definisjon.forKode(req.avklaringsbehovKode!!) else null

        val regelInput = RegelInput(
            callId = callId,
            ansattIdent = ansattIdent,
            currentToken = token,
            roller = roller,
            søkerIdenter = identer,
            avklaringsbehovFraBehandlingsflyt = avklaringsbehov,
            avklaringsbehovFraPostmottak = null,
            operasjon = req.operasjon
        )
        return regelService.vurderTilgang(regelInput)
    }

    suspend fun harTilgangTilJournalpost(
        ansattIdent: String,
        req: JournalpostTilgangRequest,
        roller: List<Rolle>,
        token: String,
        callId: String
    ): Boolean {
        val journalpostInfo: SafJournalpost = safClient.hentJournalpostInfo(req.journalpostId, callId)
        val identer = finnIdenterForJournalpost(journalpostInfo, token)
        val avklaringsbehov =
            if (req.avklaringsbehovKode != null) PostmottakDefinisjon.forKode(req.avklaringsbehovKode!!) else null

        val regelInput = RegelInput(
            callId = callId,
            ansattIdent = ansattIdent,
            currentToken = token,
            roller = roller,
            søkerIdenter = identer,
            avklaringsbehovFraBehandlingsflyt = null,
            avklaringsbehovFraPostmottak = avklaringsbehov,
            operasjon = req.operasjon
        )
        return regelService.vurderTilgang(regelInput)
    }
    
    private suspend fun finnIdenterForJournalpost(journalpost: SafJournalpost, token: String): IdenterRespons {
        val saksnummer = journalpost.sak?.fagsakId
        if(saksnummer != null) {
            return behandlingsflytClient.hentIdenterForSak(saksnummer, token)
        } else {
            val søkerIdent = journalpost.bruker?.id
            requireNotNull(søkerIdent)
            return IdenterRespons(søker = listOf(søkerIdent), barn = emptyList())
        }
    }
}