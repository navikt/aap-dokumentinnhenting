package dokumentinnhenting.integrasjoner.azure

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface TokenProviderV2 {
    suspend fun getToken(scope: String, token: OidcToken?): String
}

object OboTokenProvider : TokenProviderV2 {
    override suspend fun getToken(scope: String, token: OidcToken?): String {
        require(token != null) {
            "Kan ikke hente OBO-token uten innkommende token"
        }

        return AzureAdGateway.getOboToken(scope, token).accessToken
    }
}

object SystemTokenProvider : TokenProviderV2 {
    override suspend fun getToken(scope: String, token: OidcToken?): String {
        return AzureAdGateway.getSystemToken(scope).accessToken
    }
}
