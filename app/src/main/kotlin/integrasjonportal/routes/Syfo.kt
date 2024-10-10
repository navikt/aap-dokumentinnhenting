package integrasjonportal.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route

fun NormalOpenAPIRoute.syfo(
) {
    route("/syfo") {
        route("/sendnoe") {
            post<Unit, Unit, Unit> { _, req ->
                //prometheus.httpCallCounter("/integrasjonportal/temp").increment()
                //respond()
            }
        }
    }
}
