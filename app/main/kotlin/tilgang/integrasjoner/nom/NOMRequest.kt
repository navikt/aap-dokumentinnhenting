package tilgang.integrasjoner.nom

import tilgang.graphql.asQuery

internal data class NOMRequest(val query: String, val variables: Variables) {
    data class Variables(val personident: String)

    companion object {
        fun hentNavIdentFraPersonIdent(personident: String) = NOMRequest(
            query = RESSURS_PERSONNUMMER_TIL_NAVIDENT_QUERY.asQuery(),
            variables = Variables(personident = personident),
        )
    }
}

private const val personident = "\$personident"
val RESSURS_PERSONNUMMER_TIL_NAVIDENT_QUERY = """
    query($personident: String!) {
        ressurs(where: {personident: $personident}) {
            navident
        }
    }
""".trimIndent()
