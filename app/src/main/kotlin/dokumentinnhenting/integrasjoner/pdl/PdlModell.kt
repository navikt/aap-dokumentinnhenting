package dokumentinnhenting.integrasjoner.pdl

import dokumentinnhenting.util.graphql.GraphQLError
import kotlin.math.min

data class PdlRequest(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String? = null,
    val identer: List<String>? = null
)

data class Ident(val identifikator: String, val aktivIdent: Boolean = true) {
    fun er(ident: Ident): Boolean {
        return identifikator == ident.identifikator
    }

    override fun toString(): String {
        return "Ident(identifikator='${maskert()}')"
    }

    fun maskert(): String =
        "${identifikator.substring(0, min(identifikator.length, 6))}*****"
}

abstract class PdlResponse(
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

class PdlIdenterDataResponse(
    val data: PdlIdenterData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : PdlResponse(errors, extensions)


data class PdlIdenterData(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: PdlGruppe
)

enum class PdlGruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}

@Suppress("EnumEntryName")
enum class PersonStatus {
    bosatt,
    utflyttet,
    forsvunnet,
    doed,
    opphort,
    foedselsregistrert,
    ikkeBosatt,
    midlertidig,
    inaktiv
}

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)