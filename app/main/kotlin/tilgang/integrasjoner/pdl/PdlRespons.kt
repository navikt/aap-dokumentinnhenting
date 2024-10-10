package tilgang.integrasjoner.pdl

import tilgang.graphql.GraphQLError
import tilgang.graphql.GraphQLExtensions

data class PersonResultat(
    val ident: String,
    val adressebeskyttelse: List<Gradering>,
    val code: String
)

internal data class PdlResponse(
    val data: PdlData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

internal data class PdlData(
    val hentGeografiskTilknytning: HentGeografiskTilknytningResult?,
    val hentPerson: PdlPerson?,
    val hentPersonBolk: List<HentPersonBolkResult>?
)

internal data class HentPersonBolkResult(
    val ident: String,
    val person: PdlPerson?,
    val code: String,
)

internal data class PdlPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>?,
    val code: Code?     //Denne er påkrevd ved hentPersonBolk
)

data class HentGeografiskTilknytningResult(
    val gtType: PdlGeoType,
    val gtKommune: String?,
    val gtBydel: String?,
    val gtLand: String?
)

enum class PdlGeoType {
    BYDEL, KOMMUNE, UDEFINERT, UTLAND
}

internal enum class Code {
    ok, not_found, bad_request //TODO: add more
}

internal data class PdlVegadresse(
    val adressenavn: String,
    val husbokstav: String?,
    val husnummer: String?,
    val postnummer: String,
)

internal data class Adressebeskyttelse(
    val gradering: Gradering
)

enum class Gradering {
    FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    UGRADERT
}