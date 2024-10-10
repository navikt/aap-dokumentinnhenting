package integrasjonportal.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.request.*
import integrasjonportal.*

fun NormalOpenAPIRoute.integrasjonportal(
) {
    route("/integrasjonportal") {
        route("/temp") {
            post<Unit, Unit, Unit> { _, req ->
                //prometheus.httpCallCounter("/integrasjonportal/temp").increment()
                //respond()
            }
        }
    }
}
