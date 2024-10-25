package dokumentinnhenting.util.auth

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineContext
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun OpenAPIPipelineContext.navIdent(): String {
    return requireNotNull(pipeline.call.principal<JWTPrincipal>()) {
        "principal mangler i ktor auth"
    }.getClaim("NAVident", String::class)
        ?: error("Ident mangler i token claims")
}
