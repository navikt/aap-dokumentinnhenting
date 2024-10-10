package tilgang.graphql

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

class GraphQLWarning(
    val query:String?,
    val id:String?,
    val code: String?,
    val message: String?,
    val details: String?,
)
