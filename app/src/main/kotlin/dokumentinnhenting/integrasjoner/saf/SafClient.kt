package dokumentinnhenting.integrasjoner.saf

import java.net.URI
import java.time.LocalDateTime
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider

object SafClient {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        responseHandler = SafResponseHandler()
    )

    fun hentDokumenterForSak(saksnummer: Saksnummer, token: OidcToken): List<Journalpost> {
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktFagsak.graphql"),
            variables = DokumentoversiktFagsakVariables(saksnummer.toString())
        )

        val httpRequest = PostRequest(body = request, currentToken = token)
        val response: SafDokumentoversiktFagsakDataResponse =
            requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))

        return response.data?.dokumentoversiktFagsak?.journalposter ?: emptyList()
    }

    fun hentDokumenterForBruker(ident: String, token: OidcToken): List<Journalpost> {
        // TODO: Støtte filtrering fra frontend
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktBruker.graphql"),
            variables = DokumentoversiktBrukerVariables(
                brukerId = BrukerId(ident, BrukerId.BrukerIdType.FNR),
                tema = listOf("AAP"),
                journalposttyper = emptyList(),
                journalstatuser = emptyList(),
                foerste = 100,
            )
        )

        val httpRequest = PostRequest(body = request, currentToken = token)
        val response: SafDokumentoversiktBrukerDataResponse =
            requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))

        return response.data?.dokumentoversiktBruker?.journalposter ?: return emptyList()
    }

    private fun getQuery(name: String): String {
        val resource = javaClass.getResource(name)
            ?: throw InternfeilException("Kunne ikke opprette spørring mot SAF")

        return resource.readText().replace(Regex("[\n\t]"), "")
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

data class KopierJournalpost(
    val journalpostId: String,
    val tittel: String,
    val personIdent: String,
    val fagsakId: String,
)
