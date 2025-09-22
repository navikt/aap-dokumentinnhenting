package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.util.graphql.ErrorCode
import dokumentinnhenting.util.graphql.GraphQLError
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler

class SafResponseHandler() : RestResponseHandler<InputStream> {

    private val defaultResponseHandler = DefaultResponseHandler()

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R,
    ): R? {
        val respons = defaultResponseHandler.håndter(request, response, mapper)

        if (respons is SafResponse && !respons.errors.isNullOrEmpty()) {
            throw mapSafException(respons.errors)
        }

        return respons
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

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return defaultResponseHandler.bodyHandler()
    }
}
