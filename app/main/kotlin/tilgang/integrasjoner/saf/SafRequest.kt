package no.nav.aap.postmottak.saf.graphql

import tilgang.graphql.asQuery

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val journalpostId: String? = null)

    companion object {
        fun hentJournalpost(journalpostId: Long) = SafRequest(
            query = journalpost.asQuery(),
            variables = Variables(journalpostId = journalpostId.toString())
        )
    }
}

private const val journalpostId = "\$journalpostId"

private val journalpost = """
    query($journalpostId: String!) {
        journalpost(journalpostId: $journalpostId) {
            journalpostId
            sak {
                fagsakId
                fagsaksystem
                sakstype
                tema
            }
            bruker {
                id
                type
            }
        }
    }
""".trimIndent()

