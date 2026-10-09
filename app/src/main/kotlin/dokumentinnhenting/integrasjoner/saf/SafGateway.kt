package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import dokumentinnhenting.util.graphql.ErrorCode
import dokumentinnhenting.util.graphql.GraphQLError
import dokumentinnhenting.util.metrics.prometheus
import dokumentinnhenting.util.metrics.recordSafBrukerJournalposterCount
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

object SafGateway {
    private val graphqlUrl = requiredConfigForKey("INTEGRASJON_SAF_URL_GRAPHQL")
    private val scope = requiredConfigForKey("INTEGRASJON_SAF_SCOPE")

    suspend fun hentDokumenterForSak(saksnummer: Saksnummer, token: OidcToken): List<Journalpost> {
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktFagsak.graphql"),
            variables = DokumentoversiktFagsakVariables(saksnummer.toString())
        )

        val response = defaultHttpClient.post(graphqlUrl) {
            bearerAuth(OboTokenProvider.getToken(scope, token))
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<SafDokumentoversiktFagsakDataResponse>()

        if (response.errors != null) {
            throw mapSafException(response.errors)
        }

        return response.data?.dokumentoversiktFagsak?.journalposter.orEmpty()
    }

    suspend fun hentDokumenterForJournalpost(journalpostId: JournalpostId, token: OidcToken): Journalpost? {
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktJournalpost.graphql"),
            variables = DokumentoversiktJournalpostVariables(journalpostId.identifikator)
        )

        val response = defaultHttpClient.post(graphqlUrl) {
            bearerAuth(OboTokenProvider.getToken(scope, token))
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<SafDokumentoversiktJournalpostDokumenterResponse>()

        if (response.errors != null) {
            throw mapSafException(response.errors)
        }

        return response.data?.journalpost
    }

    suspend fun hentDokumenterForBruker(
        ident: String,
        tema: List<String> = listOf("AAP"),
        typer: List<Journalposttype> = emptyList(),
        statuser: List<Journalstatus> = emptyList(),
        token: OidcToken,
    ): List<Journalpost> {
        val query = getQuery("/saf/dokumentoversiktBruker.graphql")
        val journalposter = hentAlleBrukerDokumentoversiktSider { etter ->
            val request = SafRequest(
                query = query,
                variables = DokumentoversiktBrukerVariables(
                    brukerId = BrukerId(ident, BrukerId.BrukerIdType.FNR),
                    tema = tema.takeUnless(List<String>::isEmpty) ?: listOf("AAP"),
                    journalposttyper = typer,
                    journalstatuser = statuser,
                    foerste = 100,
                    etter = etter,
                )
            )

            val response = defaultHttpClient.post(graphqlUrl) {
                bearerAuth(OboTokenProvider.getToken(scope, token))
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<SafDokumentoversiktBrukerDataResponse>()

            if (response.errors != null) {
                throw mapSafException(response.errors)
            }

            response.data?.dokumentoversiktBruker
                ?: throw InternfeilException("Mangler dokumentoversikt i respons fra SAF.")
        }
        prometheus.recordSafBrukerJournalposterCount(journalposter.size)
        return journalposter
    }

    private fun getQuery(name: String): String {
        val resource = javaClass.getResource(name)
            ?: throw InternfeilException("Kunne ikke opprette spørring mot SAF")

        return resource.readText().replace(Regex("[\n\t]"), "")
    }

    private fun mapSafException(errors: List<GraphQLError>): ApiException {
        val error = errors.first()
        return when (error.extensions.code) {
            ErrorCode.FORBIDDEN -> IkkeTillattException("Mangler tilgang til å se brukerens journalposter.")
            ErrorCode.NOT_FOUND -> VerdiIkkeFunnetException("Fant ingen journalpost.")
            ErrorCode.BAD_REQUEST -> UgyldigForespørselException("Ugyldig forespørsel mot arkivet. Hvis problemet vedvarer, opprett sak i Porten.")
            ErrorCode.SERVER_ERROR -> InternfeilException("Teknisk feil i Saf. Prøv igjen om litt.")
            else -> InternfeilException("Ukjent feil oppsto ved henting av dokument(er) fra arkivet.")
        }
    }
}

internal suspend fun hentAlleBrukerDokumentoversiktSider(
    hentSide: suspend (etter: String?) -> DokumentoversiktBruker,
): List<Journalpost> {
    val journalposter = mutableListOf<Journalpost>()
    val brukteSluttpekere = mutableSetOf<String>()
    var etter: String? = null

    while (true) {
        val side = hentSide(etter)
        journalposter.addAll(side.journalposter)

        if (!side.sideInfo.finnesNesteSide) {
            return journalposter
        }

        val sluttpeker = side.sideInfo.sluttpeker
            ?: throw InternfeilException("SAF oppgir flere sider, men mangler sluttpeker.")
        if (sluttpeker == etter || !brukteSluttpekere.add(sluttpeker)) {
            throw InternfeilException("SAF returnerte en gjentatt sluttpeker for dokumentoversikten.")
        }
        etter = sluttpeker
    }
}

data class Doc(
    val tema: String,
    val dokumentInfoId: String,
    val journalpostId: String,
    val brevkode: String?,
    val tittel: String,
    val erUtgående: Boolean,
    val datoOpprettet: LocalDateTime,
    val variantformat: Variantformat,
)
