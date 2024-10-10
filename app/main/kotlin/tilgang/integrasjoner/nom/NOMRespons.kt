package tilgang.integrasjoner.nom;

import tilgang.graphql.GraphQLError

internal data class NOMRespons (
    val errors: List<GraphQLError>?,
    val data: NOMData?
)

internal data class NOMData(
    val ressurs: Ressurs?
)

internal data class Ressurs(
    val navident: String,
    val personident: String
)