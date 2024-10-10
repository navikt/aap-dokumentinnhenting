package tilgang.regler

import tilgang.geo.GeoRolle
import tilgang.geo.GeoService
import tilgang.geo.GeoType
import tilgang.integrasjoner.pdl.HentGeografiskTilknytningResult
import tilgang.integrasjoner.pdl.IPdlGraphQLClient
import tilgang.integrasjoner.pdl.PdlGeoType

data object GeoRegel : Regel<GeoInput> {

    override fun vurder(input: GeoInput): Boolean {
        val (geoRoller, søkersGeografiskeTilknytning) = input
        val harNasjonalTilgang = geoRoller.any { it.geoType === GeoType.NASJONAL }

        return when (søkersGeografiskeTilknytning.gtType) {
            PdlGeoType.KOMMUNE -> harNasjonalTilgang
                    || søkersGeografiskeTilknytning.gtKommune in geoRoller.filter { it.geoType === GeoType.KOMMUNE }
                .map { it.kode }

            PdlGeoType.BYDEL -> harNasjonalTilgang
                    || harRettigheterTilBydel(requireNotNull(søkersGeografiskeTilknytning.gtBydel), geoRoller)

            PdlGeoType.UTLAND -> geoRoller.any { it.geoType == GeoType.UTLAND }
            PdlGeoType.UDEFINERT -> geoRoller.any { it.geoType == GeoType.UDEFINERT }
        }
    }

    private fun harRettigheterTilBydel(bydel: String, geoRoller: List<GeoRolle>): Boolean {
        val kommune = bydel.substring(0, 4)

        return listOf(bydel, kommune).any {
            it in geoRoller
                .filter { it.geoType === GeoType.BYDEL || it.geoType === GeoType.KOMMUNE }
                .map { it.kode }
        }
    }
}

class GeoInputGenerator(
    private val geoService: GeoService,
    private val pdlClient: IPdlGraphQLClient
) :
    InputGenerator<GeoInput> {
    override suspend fun generer(input: RegelInput): GeoInput {
        val geoRoller = geoService.hentGeoRoller(input.currentToken, input.ansattIdent)
        val søkersGeografiskeTilknytning = requireNotNull(
            pdlClient.hentGeografiskTilknytning(
                input.søkerIdenter.søker.first(),
                input.callId
            )
        )
        return GeoInput(geoRoller, søkersGeografiskeTilknytning, input.ansattIdent)
    }
}

data class GeoInput(
    val geoRoller: List<GeoRolle>,
    val søkersGeografiskTilknytning: HentGeografiskTilknytningResult,
    val ident: String
)