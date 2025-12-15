package dokumentinnhenting.integrasjoner.azure

interface TokenProviderV2 {
    suspend fun getToken(scope: String, token: String?): String
}

object OboTokenProvider : TokenProviderV2 {
    override suspend fun getToken(scope: String, token: String?): String {
        require(token != null) {
            "Kan ikke hente OBO-token uten innkommende token"
        }

        return AzureAdGateway.getOboToken(scope, token).accessToken
    }
}

object SystemTokenProvider : TokenProviderV2 {
    override suspend fun getToken(scope: String, token: String?): String {
        return AzureAdGateway.getSystemToken(scope).accessToken
    }
}
