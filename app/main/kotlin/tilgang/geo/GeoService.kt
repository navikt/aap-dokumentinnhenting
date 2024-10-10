package tilgang.geo

import tilgang.integrasjoner.msgraph.IMsGraphClient

class GeoService(private val msGraphClient: IMsGraphClient) {

    suspend fun hentGeoRoller(currentToken: String, ident: String): List<GeoRolle> {
        return msGraphClient.hentAdGrupper(currentToken, ident).groups
            .filter { it.name.startsWith(GEO_GROUP_PREFIX) }
            .map { parseGeoRolle(it.name) }
    }

    private fun parseGeoRolle(rolleNavn: String): GeoRolle {
        val kode = rolleNavn.removePrefix("${GEO_GROUP_PREFIX}_")
        return when (kode) {
            "UTLAND" -> GeoRolle(GeoType.UTLAND, null)
            "UDEFINERT" -> GeoRolle(GeoType.UDEFINERT, null)
            "NASJONAL" -> GeoRolle(GeoType.NASJONAL, null)
            else -> parseKode(kode)
        }
    }

    private fun parseKode(kode: String): GeoRolle {
        if (kode.length == 4) {
            return GeoRolle(GeoType.KOMMUNE, kode)
        } else if (kode.length == 6) {
            return GeoRolle(GeoType.BYDEL, kode)
        } else error("Klarte ikke parse geokode")
    }

    companion object {
        const val GEO_GROUP_PREFIX = "0000-GA-GEO"
    }

}

data class GeoRolle(val geoType: GeoType, val kode: String?)
enum class GeoType {
    NASJONAL,
    UDEFINERT,
    UTLAND,
    KOMMUNE,
    BYDEL
}