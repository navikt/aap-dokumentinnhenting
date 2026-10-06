@file:Suppress("GraphQLUnresolvedReference")

package dokumentinnhenting.integrasjoner.pdl

import org.intellij.lang.annotations.Language

class PdlIdentGateway {

    fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        val request = PdlRequest(IDENT_QUERY, IdentVariables(ident.identifikator))
        val response: PdlIdenterDataResponse = PdlGateway.query(request)

        return response.data
            ?.hentIdenter
            ?.identer
            ?.filter { it.gruppe == PdlGruppe.FOLKEREGISTERIDENT }
            ?.map { Ident(identifikator = it.ident, aktivIdent = it.historisk.not()) }
            .orEmpty()
    }
}


@Language("GraphQL")
val IDENT_QUERY = $$"""
    query($ident: ID!) {
        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
""".trimIndent()

