package dokumentinnhenting.integrasjoner.pdl

import dokumentinnhenting.util.graphql.GraphQLError

data class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)