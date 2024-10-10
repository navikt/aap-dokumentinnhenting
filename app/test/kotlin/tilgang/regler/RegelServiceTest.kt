package tilgang.regler

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tilgang.Fakes
import tilgang.Operasjon
import tilgang.SkjermingConfig
import tilgang.auth.AzureConfig
import tilgang.enhet.EnhetService
import tilgang.geo.GeoService
import tilgang.integrasjoner.behandlingsflyt.IdenterRespons
import tilgang.integrasjoner.msgraph.Group
import tilgang.integrasjoner.msgraph.IMsGraphClient
import tilgang.integrasjoner.msgraph.MemberOf
import tilgang.integrasjoner.nom.INOMClient
import tilgang.integrasjoner.pdl.HentGeografiskTilknytningResult
import tilgang.integrasjoner.pdl.IPdlGraphQLClient
import tilgang.integrasjoner.pdl.PdlGeoType
import tilgang.integrasjoner.pdl.PersonResultat
import tilgang.integrasjoner.skjerming.SkjermingClient
import java.net.URI
import java.util.*

class RegelServiceTest {
    @ParameterizedTest
    @EnumSource(Definisjon::class)
    fun `skal alltid gi false når roller er tom array`(avklaringsbehov: Definisjon) {
        Fakes().use {
            val graphClient = object : IMsGraphClient {
                override suspend fun hentAdGrupper(currentToken: String, ident: String): MemberOf {
                    return MemberOf(groups = listOf(Group(id = "0", name = "000-GA-GEO-abc")))
                }
            }

            val geoService = GeoService(graphClient)
            val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val pdlService = object : IPdlGraphQLClient {
                override suspend fun hentPersonBolk(personidenter: List<String>, callId: String): List<PersonResultat> {
                    return personidenter.map { PersonResultat(ident = it, adressebeskyttelse = listOf(), code = "XXX") }
                }

                override suspend fun hentGeografiskTilknytning(
                    ident: String,
                    callId: String
                ): HentGeografiskTilknytningResult {
                    return HentGeografiskTilknytningResult(
                        gtType = PdlGeoType.KOMMUNE,
                        gtLand = "NOR",
                        gtBydel = null,
                        gtKommune = "fff"
                    )
                }
            }
            val enhetService = EnhetService(graphClient)
            val skjermingClient = object : SkjermingClient(
                AzureConfig(
                    clientId = "",
                    clientSecret = "",
                    tokenEndpoint = URI.create("http://localhost:1234").resolve("/token").toURL(),
                    jwks = URI.create("http://localhost:1234").resolve("/jwks").toURL(),
                    issuer = ""
                ), SkjermingConfig("skjerming_base_url", "skjerming_scope"), it.redis, prometheus
            ) {
            }

            val nomClient = object : INOMClient {
                override suspend fun personNummerTilNavIdent(søkerIdent: String, callId: String): String {
                    return "T131785"
                }
            }
            val regelService = RegelService(geoService, enhetService, pdlService, skjermingClient, nomClient)

            runBlocking {
                val svar = regelService.vurderTilgang(
                    RegelInput(
                        callId = UUID.randomUUID().toString(),
                        ansattIdent = "123",
                        avklaringsbehovFraBehandlingsflyt = avklaringsbehov,
                        avklaringsbehovFraPostmottak = null,
                        currentToken = "xxx",
                        søkerIdenter = IdenterRespons(søker = listOf("423"), barn = listOf()),
                        operasjon = Operasjon.SAKSBEHANDLE,
                        roller = listOf()
                    )
                )
                Assertions.assertFalse(svar)
            }
        }
    }
}