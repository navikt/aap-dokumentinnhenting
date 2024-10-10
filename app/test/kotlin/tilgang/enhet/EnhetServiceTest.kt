package tilgang.enhet

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tilgang.integrasjoner.msgraph.Group
import tilgang.integrasjoner.msgraph.IMsGraphClient
import tilgang.integrasjoner.msgraph.MemberOf

class EnhetServiceTest {
    @Test
    fun `lister kun opp enhets-roller`() {
        val graphClient = object : IMsGraphClient {
            override suspend fun hentAdGrupper(currentToken: String, ident: String): MemberOf {
                return MemberOf(
                    groups = listOf(
                        Group(name = "0000-GA-ENHET_12345", id = "xxx"),
                        Group(name = "0000-GA-GEO_12345", id = "xxx")
                    )
                )
            }

        }
        val service = EnhetService(graphClient)

        runBlocking {
            val res = service.hentEnhetRoller("xxx", "")
            assertThat(res).isNotEmpty()
            assertThat(res).hasSize(1)
            assertThat(res[0].kode).isEqualTo("12345")
        }
    }
}