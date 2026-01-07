package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.defaultHttpClient
import dokumentinnhenting.integrasjoner.azure.OboTokenProvider
import dokumentinnhenting.util.graphql.ErrorCode
import dokumentinnhenting.util.graphql.GraphQLError
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.LocalDateTime
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

object SafGateway {
    private val graphqlUrl = requiredConfigForKey("integrasjon.saf.url.graphql")
    private val scope = requiredConfigForKey("integrasjon.saf.scope")

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

    suspend fun hentDokumenterForBruker(
        ident: String,
        tema: List<String> = listOf("AAP"),
        typer: List<Journalposttype> = emptyList(),
        statuser: List<Journalstatus> = emptyList(),
        token: OidcToken,
    ): List<Journalpost> {
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktBruker.graphql"),
            variables = DokumentoversiktBrukerVariables(
                brukerId = BrukerId(ident, BrukerId.BrukerIdType.FNR),
                tema = tema.takeUnless(List<String>::isEmpty) ?: listOf("AAP"),
                journalposttyper = typer,
                journalstatuser = statuser,
                foerste = 100,
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

        return response.data?.dokumentoversiktBruker?.journalposter.orEmpty()
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
