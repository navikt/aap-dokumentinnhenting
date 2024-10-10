import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import tilgang.BehandlingTilgangRequest
import tilgang.JournalpostTilgangRequest
import tilgang.SakTilgangRequest
import tilgang.TilgangResponse
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val azure = embeddedServer(Netty, port = azurePort, module = { azureFake() }).start()
    private val tilgang = embeddedServer(Netty, port = 0, module = {    tilgangFake() }).apply { start() }

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/token") {
                val token = AzureTokenGen("behandlingsflyt", "behandlingsflyt").generate()
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }

    private fun Application.tilgangFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@tilgangFake.log.info(
                    "TILGANG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/tilgang/sak") {
                // TODO: Test kontrakten på en litt mer fornuftig måte
                val req = call.receive<SakTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
            post("/tilgang/behandling") {
                val req = call.receive<BehandlingTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
            post("/tilgang/journalpost") {
                val req = call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }

    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

    init {
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")

        // Tilgang
        System.setProperty("integrasjon.tilgang.url", "http://localhost:${tilgang.port()}")
        System.setProperty("integrasjon.tilgang.scope", "scope")
        System.setProperty("integrasjon.tilgang.azp", "azp")
    }

    override fun close() {
        azure.stop(0L, 0L)
    }
}

internal data class TestToken(
    val access_token: String,
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)

internal class AzureTokenGen(private val issuer: String, private val audience: String) {
    private val rsaKey: RSAKey = JWKSet.parse(AZURE_JWKS).getKeyByKeyId("localhost-signer") as RSAKey

    private fun signed(claims: JWTClaimsSet): SignedJWT {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).type(JOSEObjectType.JWT).build()
        val signer = RSASSASigner(rsaKey.toPrivateKey())
        val signedJWT = SignedJWT(header, claims)
        signedJWT.sign(signer)
        return signedJWT
    }

    private fun claims(): JWTClaimsSet {
        return JWTClaimsSet
            .Builder()
            .issuer(issuer)
            .audience(audience)
            .expirationTime(LocalDateTime.now().plusHours(4).toDate())
            .claim("NAVident", "Lokalsaksbehandler")
            .claim("azp_name", "azp")
            .build()
    }

    private fun LocalDateTime.toDate(): Date {
        return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
    }

    fun generate(): String {
        return signed(claims()).serialize()
    }
}

@Language("JSON")
internal const val AZURE_JWKS: String = """{
  "keys": [
    {
      "kty": "RSA",
      "d": "O4HE82G7UP-KVIryTboX-VqbxBbSo16_shQ-zIGUiHo0DVoTBJYfmRWSIx4bPT-n80imaYhohHd79UO1lqWMF-GrZdFJaYjU7yzKGc_W7Pw5QVVng9JZRlgIuz_L7Zl-q3R1gV0-FZWhRZtkhIbETl8216cBFjSrUVF04Fpv4n9dBV3ySgjfG_0MMuysAWx6gZFyP2g1IOnuCY7v32kLR9wdLWPSKFz-icm66AR5DX0hyMdUuwQ56DEBAzf6-1MqznqKiwg-whL6zcHHLdaWzj02J8bMLpeZ9PylbxdTHEWdMP6HaXNdqVMx920UnWmCVVcFOIxs53PdGnyJThVPEQ",
      "e": "AQAB",
      "use": "sig",
      "kid": "localhost-signer",
      "alg": "RS256",
      "n": "wQkxSymiZJ8k4zBTo8HhjmvMB-OZl6F1qg_ZsPXwfa8jTzxbxkicAAPKowh7T0vT_dQAR_Vhy9G6v2jkUUnlbvxULqOt395TTUEB-MBPb0gxIk9O65Ws9eRj12hWo6gDaHBuxWEEjzvVHEDAmqHs7mswoY7nkn2ktxYDPdCjKystyCyR6TCMxkOMXLt0gUfdZyGir60d4ZsGeSIV66L2_pGI0qsEELGvXCLKQe7-UceyYioxmjRs_GGl8Zd1psSiXiZYXHUYIIslZakPUPNUM5_2eFwTbwQPybhJ0WLqUxWEGfoZjyMflR0FTTo5ZLOKLZAsXCpZlR7nY_tuMNWWhw"
    }
  ]
}"""

data class ErrorRespons(val message: String?)
