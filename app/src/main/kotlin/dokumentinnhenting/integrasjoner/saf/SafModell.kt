package dokumentinnhenting.integrasjoner.saf

import dokumentinnhenting.util.graphql.GraphQLError
import dokumentinnhenting.util.graphql.GraphQLExtensions
import java.time.LocalDateTime

interface Variables

data class SafRequest(val query: String, val variables: Variables)

data class DokumentoversiktFagsakVariables(val fagsakId: String) : Variables

data class DokumentoversiktBrukerVariables(
    val brukerId: BrukerId,
    val tema: List<String>,
    val journalposttyper: List<Journalposttype>,
    val journalstatuser: List<Journalstatus>,
    val foerste: Int,
    val etter: String? = null,
) : Variables

data class BrukerId(
    val id: String,
    val type: BrukerIdType,
) {
    enum class BrukerIdType {
        AKTOERID,
        FNR,
        ORGNR,
    }

    override fun toString(): String {
        val maskertId = if (id.length >= 6) id.substring(0..5) + "*****" else "*****"

        return "BrukerId(id=${maskertId}, type=$type)"
    }
}

abstract class SafResponse(
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?,
)

class SafDokumentoversiktFagsakDataResponse(
    val data: SafDokumentversiktFagsakData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?,
) : SafResponse(errors, extensions)

class SafDokumentoversiktBrukerDataResponse(
    val data: SafDokumentversiktBrukerData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?,
) : SafResponse(errors, extensions)

data class Dokumentvariant(
    val variantformat: Variantformat,
    val saksbehandlerHarTilgang: Boolean,
)

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String,
    val brevkode: String?,
    val dokumentvarianter: List<Dokumentvariant>,
)

data class Journalpost(
    val journalpostId: String,
    val dokumenter: List<DokumentInfo>,
    val tittel: String?,
    val journalposttype: Journalposttype,
    val journalstatus: Journalstatus,
    val tema: String?,
    val temanavn: String?,
    val behandlingstema: String?,
    val behandlingstemanavn: String?,
    val sak: JournalpostSak?,
    val avsenderMottaker: AvsenderMottaker?,
    val datoOpprettet: LocalDateTime?,
    val relevanteDatoer: List<RelevantDato>?,
)

data class JournalpostSak(
    val sakstype: Sakstype,
    val tema: String?,
    val fagsaksystem: String?,
    val fagsakId: String?,
) {
    enum class Sakstype {
        FAGSAK,
        GENERELL_SAK,
    }
}

data class Bruker(
    val id: BrukerId,
)

data class AvsenderMottaker(
    val id: String?,
    val type: AvsenderMottakerIdType?,
    val navn: String?,
) {
    enum class AvsenderMottakerIdType {
        FNR,
        ORGNR,
        HPRNR,
        UTL_ORG,
        NULL,
        UKJENT,
    }
}

data class RelevantDato(
    val dato: LocalDateTime,
    val datotype: Datotype,
) {
    enum class Datotype {
        DATO_SENDT_PRINT,
        DATO_EKSPEDERT,
        DATO_JOURNALFOERT,
        DATO_REGISTRERT,
        DATO_AVS_RETUR,
        DATO_DOKUMENT,
        DATO_LEST,
    }
}

data class DokumentoversiktFagsak(val journalposter: List<Journalpost>)
data class SafDokumentversiktFagsakData(val dokumentoversiktFagsak: DokumentoversiktFagsak?)

data class DokumentoversiktBruker(val journalposter: List<Journalpost>)
data class SafDokumentversiktBrukerData(val dokumentoversiktBruker: DokumentoversiktBruker?)

enum class Variantformat {
    ARKIV,
    FULLVERSJON,
    PRODUKSJON,
    PRODUKSJON_DLF,
    SLADDET,
    ORIGINAL,
}

enum class Journalposttype {
    I, U, N
}

enum class Journalstatus {
    MOTTATT,
    JOURNALFOERT,
    FERDIGSTILT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    UTGAAR,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
    UKJENT
}
