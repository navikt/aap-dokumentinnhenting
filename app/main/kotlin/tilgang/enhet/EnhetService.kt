package tilgang.enhet

import tilgang.integrasjoner.msgraph.IMsGraphClient

class EnhetService(private val msGraphClient: IMsGraphClient) {

    suspend fun hentEnhetRoller(currentToken: String, ident: String): List<EnhetRolle> {
        return msGraphClient.hentAdGrupper(currentToken, ident).groups
            .filter { it.name.startsWith(ENHET_GROUP_PREFIX) }
            .map { parseEnhetRolle(it.name) }
    }

    private fun parseEnhetRolle(rolleNavn: String): EnhetRolle {
        val kode = rolleNavn.removePrefix(ENHET_GROUP_PREFIX)
        return EnhetRolle(kode = kode)
    }

    companion object {
        const val ENHET_GROUP_PREFIX = "0000-GA-ENHET_"
    }

}

data class EnhetRolle(val kode: String)