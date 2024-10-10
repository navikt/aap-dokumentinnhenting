package tilgang.regler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tilgang.geo.GeoRolle
import tilgang.geo.GeoType
import tilgang.integrasjoner.pdl.HentGeografiskTilknytningResult
import tilgang.integrasjoner.pdl.PdlGeoType

class GeoRegelTest {
    @ParameterizedTest(name = "Skal returnere {2} for roller {1} og geografisk tilknytning {0}")
    @MethodSource("testData")
    fun geoTest(
        geografiskTilknytning: HentGeografiskTilknytningResult,
        roller: List<GeoRolle>,
        forventet: Boolean
    ) {
        assertEquals(forventet, GeoRegel.vurder(GeoInput(roller, geografiskTilknytning, "")))
    }

    companion object {
        @JvmStatic
        fun testData(): List<Arguments> {
            val osloBydelTilknytning = HentGeografiskTilknytningResult(
                PdlGeoType.BYDEL, null, "030102", null
            )
            val trondheimBydelTilknytning = HentGeografiskTilknytningResult(
                PdlGeoType.BYDEL, null, "500101", null
            )
            val osloKommuneTilknytning = HentGeografiskTilknytningResult(
                PdlGeoType.KOMMUNE, "0301", null, null
            )
            val nordreFolloTilknytning = HentGeografiskTilknytningResult(
                PdlGeoType.KOMMUNE, "3207", null, null
            )
            val utlandTilknytning = HentGeografiskTilknytningResult(PdlGeoType.UTLAND, null, null, null)
            val udefinertTilknytning = HentGeografiskTilknytningResult(PdlGeoType.UDEFINERT, null, null, null)

            val osloKommuneRolle = GeoRolle(GeoType.KOMMUNE, "0301")
            val utlandRolle = GeoRolle(GeoType.UTLAND, null)
            val nasjonalRolle = GeoRolle(GeoType.NASJONAL, null)
            val udefinertRolle = GeoRolle(GeoType.UDEFINERT, null)
            return listOf(
                Arguments.of(osloBydelTilknytning, listOf(osloKommuneRolle), true),
                Arguments.of(osloKommuneTilknytning, listOf(osloKommuneRolle), true),
                Arguments.of(nordreFolloTilknytning, listOf(osloKommuneRolle), false),
                Arguments.of(trondheimBydelTilknytning, listOf(osloKommuneRolle), false),
                Arguments.of(utlandTilknytning, listOf(osloKommuneRolle), false),
                Arguments.of(udefinertTilknytning, listOf(osloKommuneRolle), false),
                Arguments.of(osloKommuneTilknytning, listOf(utlandRolle), false),
                Arguments.of(utlandTilknytning, listOf(utlandRolle), true),
                Arguments.of(udefinertTilknytning, listOf(utlandRolle), false),
                Arguments.of(osloBydelTilknytning, listOf(nasjonalRolle), true),
                Arguments.of(osloKommuneTilknytning, listOf(nasjonalRolle), true),
                Arguments.of(utlandTilknytning, listOf(nasjonalRolle), false),
                Arguments.of(udefinertTilknytning, listOf(nasjonalRolle), false),
                Arguments.of(udefinertTilknytning, listOf(udefinertRolle), true),
                Arguments.of(osloBydelTilknytning, listOf(udefinertRolle), false),
                Arguments.of(utlandTilknytning, listOf(udefinertRolle), false),
                Arguments.of(osloBydelTilknytning, emptyList<GeoRolle>(), false),
                Arguments.of(osloKommuneTilknytning, emptyList<GeoRolle>(), false),
                Arguments.of(utlandTilknytning, emptyList<GeoRolle>(), false),
                Arguments.of(udefinertTilknytning, emptyList<GeoRolle>(), false),
            )
        }
    }
}