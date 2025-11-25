package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.http.HttpClientFactory
import dokumentinnhenting.http.OnBehalfOfTokenProvider
import dokumentinnhenting.util.graphql.ErrorCode
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import java.time.LocalDateTime

object SafClient {
    private val graphqlUrl = requiredConfigForKey("integrasjon.saf.url.graphql")
    private val scope = requiredConfigForKey("integrasjon.saf.scope")
    private val httpClient = HttpClientFactory.create()

    fun hentDokumenterForSak(saksnummer: Saksnummer, token: String): List<Journalpost> {
        return runBlocking {
            val exchangedToken = OnBehalfOfTokenProvider.getToken(scope, token)
            val request = SafRequest(
                query = getQuery("/saf/dokumentoversiktFagsak.graphql"),
                variables = DokumentoversiktFagsakVariables(saksnummer.toString())
            )

            val httpResponse = httpClient.post(graphqlUrl) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(exchangedToken)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                throw RuntimeException("SAF request failed: ${httpResponse.status} - ${httpResponse.bodyAsText()}")
            }

            val response: SafDokumentoversiktFagsakDataResponse = httpResponse.body()
            handleSafErrors(response.errors)
            response.data?.dokumentoversiktFagsak?.journalposter.orEmpty()
        }
    }

    fun hentDokumenterForBruker(
        ident: String,
        tema: List<String> = listOf("AAP"),
        typer: List<Journalposttype> = emptyList(),
        statuser: List<Journalstatus> = emptyList(),
        token: String
    ): List<Journalpost> {
        return runBlocking {
            val exchangedToken = OnBehalfOfTokenProvider.getToken(scope, token)
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

            val httpResponse = httpClient.post(graphqlUrl) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                bearerAuth(exchangedToken)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                throw RuntimeException("SAF request failed: ${httpResponse.status} - ${httpResponse.bodyAsText()}")
            }

            val response: SafDokumentoversiktBrukerDataResponse = httpResponse.body()
            handleSafErrors(response.errors)
            response.data?.dokumentoversiktBruker?.journalposter.orEmpty()
        }
    }

    private fun handleSafErrors(errors: List<dokumentinnhenting.util.graphql.GraphQLError>?) {
        if (!errors.isNullOrEmpty()) {
            val error = errors.first()
            when (error.extensions.code) {
                ErrorCode.FORBIDDEN -> throw SafException("Mangler tilgang til å se brukerens journalposter.")
                ErrorCode.NOT_FOUND -> throw SafException("Fant ingen journalpost.")
                ErrorCode.BAD_REQUEST -> throw SafException("Ugyldig forespørsel mot arkivet. Hvis problemet vedvarer, opprett sak i Porten.")
                ErrorCode.SERVER_ERROR -> throw SafException("Teknisk feil i Saf. Prøv igjen om litt.")
                else -> throw SafException("Ukjent feil oppsto ved henting av dokument(er) fra arkivet.")
            }
        }
    }

    private fun getQuery(name: String): String {
        val resource = SafClient::class.java.getResource(name)
            ?: throw RuntimeException("Kunne ikke opprette spørring mot SAF")

        return resource.readText().replace(Regex("[\n\t]"), "")
    }
}

class SafException(message: String) : RuntimeException(message)

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
