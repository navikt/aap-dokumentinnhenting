package no.nav.aap.postmottak.saf.graphql

import tilgang.graphql.GraphQLError
import tilgang.graphql.GraphQLExtensions
import java.time.LocalDateTime

data class SafRespons(
    val data: SafData?,
    val errors: List<GraphQLError>? = null,
    val extensions: GraphQLExtensions? = null,
) {

    fun hasErrors(): Boolean {
        return errors?.isNotEmpty() ?: false
    }
}

data class SafData(
    val journalpost: SafJournalpost?
)

data class SafJournalpost(
    /**
     * Unik identifikator per Journalpost
     * @example: 123456789
     */
    val journalpostId: Long,
    
    /**
     * Sier hvilken sak journalposten er knyttet til.
     * En journalpost kan maksimalt være knyttet til èn sak,
     * men et dokument kan være knyttet til fler journalposter og dermed fler saker.
     */
    val sak: Sak? = null,

    /**
     * Person eller org som dokumentene i journalposten gjelder.
     * Dersom journalpost er sakstilknyttet, henter SAF bruker fra GSAK/PSAK,
     * alternativt fra Joark.
     */
    val bruker: Bruker? = null,
)

data class Sak(
    /**
     * Saksnummer i fagsystemet
     */
    val fagsakId: String? = null,

    /**
     * Kode som indikerer hvilket fagsystem, evt nummerserie for fagsaker.
     */
    val fagsaksystem: String? = null,

    /**
     * Sier hvorvidt saken inngår i et fagsystem [Sakstype.FAGSAK]
     * eller ikke [Sakstype.GENERELL_SAK]
     */
    val sakstype: Sakstype,
)

enum class Sakstype {
    /**
     * Vil si at saken tilhører et fagsystem.
     * Hvilket fagsystem spesifiseres i feltet "fagsaksystem"
     */
    FAGSAK,

    /**
     * Benyttes normalt for dokumenter som ikke saksbehandles i et fagsystem.
     */
    GENERELL_SAK
}
enum class SafDatoType {
    DATO_OPPRETTET, DATO_SENDT_PRINT, DATO_EKSPEDERT,
    DATO_JOURNALFOERT, DATO_REGISTRERT,
    DATO_AVS_RETUR, DATO_DOKUMENT
}

data class Bruker(
    val id: String? = null,
    val type: BrukerIdType? = null,
)

enum class BrukerIdType {
    AKTOERID,
    FNR,
    ORGNR
}
