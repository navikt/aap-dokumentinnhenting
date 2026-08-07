package no.nav.aap.brev.util.graphql

data class GraphqlRequest<Variables>(val query: String, val variables: Variables)
