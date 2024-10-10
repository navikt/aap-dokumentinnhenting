package tilgang.integrasjoner.pdl

import tilgang.graphql.asQuery

internal data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String? = null, val identer: List<String>? = null)

    companion object {
        fun hentPersonBolk(personidenter: List<String>) = PdlRequest(
            query = PERSON_BOLK_QUERY.asQuery(),
            variables = Variables(identer = personidenter),
        )
        fun hentGeografiskTilknytning(ident: String) = PdlRequest(
            query = GEOGRAFISK_TILKNYTNING_QUERY.asQuery(),
            variables = Variables(ident = ident)
        )
    }
}


private const val identer = "\$identer"
val PERSON_BOLK_QUERY = """
    query($identer: [ID!]!) {
        hentPersonBolk(identer: $identer) {
            ident,
            person {
                adressebeskyttelse {
                    gradering
                },
            }
            code
        }
    }
""".trimIndent()

private const val ident = "\$ident"
val GEOGRAFISK_TILKNYTNING_QUERY = """
    query($ident: ID!) {
        hentGeografiskTilknytning(ident: $ident) {
            gtType
            gtKommune
            gtBydel
            gtLand
        }
}
""".trimIndent()