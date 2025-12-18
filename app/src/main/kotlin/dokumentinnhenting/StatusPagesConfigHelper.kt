package dokumentinnhenting

import com.fasterxml.jackson.core.JacksonException
import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import java.net.http.HttpTimeoutException
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.json.DeserializationException
import org.slf4j.LoggerFactory

object StatusPagesConfigHelper {
    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger(javaClass)
            val uri = call.request.local.uri

            when (cause) {
                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                /**
                 * Midlertidig for å fange opp og sikre forløpende håndtering av kjente feil
                 **/
                is ClientRequestException -> {
                    logger.error(
                        "Uhåndtert ClientRequestException (${cause.response.status}) ved kall til '$uri': ",
                        cause
                    )
                    call.respondWithError(InternfeilException("Feil ved kall til ekstern tjeneste"))
                }

                is ManglerTilgangException -> {
                    logger.warn("Mangler tilgang til å vise route: '$uri'", cause)
                    call.respondWithError(IkkeTillattException(message = "Mangler tilgang"))
                }

                is IkkeFunnetException -> {
                    logger.error("Fikk 404 fra ekstern integrasjon", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.NotFound,
                            message = "Fikk 404 fra ekstern integrasjon. Dette er mest sannsynlig en systemfeil."
                        )
                    )
                }

                is BehandlingsflytException -> {
                    logger.error(
                        "Uhåndtert feil ved kall til '{}' av type ${cause.javaClass}. Melding: ${cause.message}",
                        call.request.local.uri,
                        cause
                    )
                    call.respond(InternfeilException("Feil i behandlingsflyt: ${cause.message}"))
                }

                is JacksonException,
                is JsonConvertException,
                is DeserializationException,
                    -> {
                    logger.error("Deserialiseringsfeil ved kall til '$uri': ", cause)

                    call.respondWithError(
                        UgyldigForespørselException(message = "Deserialiseringsfeil ved kall til '$uri'")
                    )
                }

                is HttpTimeoutException -> {
                    logger.warn("Timeout ved kall til '$uri'", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.RequestTimeout,
                            message = "Forespørselen tok for lang tid. Prøv igjen om litt."
                        )
                    )
                }

                else -> {
                    logger.error("Ukjent/uhåndtert feil ved kall til '$uri' av type ${cause.javaClass}.", cause)

                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }
            }
        }
    }

    private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
        respond(
            exception.status,
            exception.tilApiErrorResponse()
        )
    }
}
