package dokumentinnhenting.integrasjoner.pdl

data class GraphqlRequest<Variables>(val query: String, val variables: Variables)
