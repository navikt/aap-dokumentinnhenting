package tilgang.regler

import tilgang.Rolle
import tilgang.integrasjoner.pdl.Gradering
import tilgang.integrasjoner.pdl.IPdlGraphQLClient
import tilgang.integrasjoner.pdl.PersonResultat

data object AdressebeskyttelseRegel : Regel<AdressebeskyttelseInput> {
    override fun vurder(input: AdressebeskyttelseInput): Boolean {
        return sjekkAdressebeskyttelse(input.roller, input.personer)
    }

    private fun sjekkAdressebeskyttelse(roller: List<Rolle>, personer: List<PersonResultat>): Boolean {
        val adresseBeskyttelse = personer.flatMap { it.adressebeskyttelse }

        return adresseBeskyttelse.isEmpty() 
                || adresseBeskyttelse.all { it === Gradering.UGRADERT }
                || (Rolle.STRENGT_FORTROLIG_ADRESSE in roller && finnStrengeste(adresseBeskyttelse) in listOf(
            Gradering.STRENGT_FORTROLIG,
            Gradering.STRENGT_FORTROLIG_UTLAND
        ))
                || (Rolle.FORTROLIG_ADRESSE in roller && finnStrengeste(adresseBeskyttelse) === Gradering.FORTROLIG)
    }

    private fun finnStrengeste(adresseBeskyttelser: List<Gradering>): Gradering {
        return when {
            Gradering.STRENGT_FORTROLIG in adresseBeskyttelser -> Gradering.STRENGT_FORTROLIG
            Gradering.STRENGT_FORTROLIG_UTLAND in adresseBeskyttelser -> Gradering.STRENGT_FORTROLIG_UTLAND
            Gradering.FORTROLIG in adresseBeskyttelser -> Gradering.FORTROLIG
            else -> Gradering.UGRADERT
        }
    }
}

data class AdressebeskyttelseInput(val roller: List<Rolle>, val personer: List<PersonResultat>)

class AdressebeskyttelseInputGenerator(private val pdlService: IPdlGraphQLClient) :
    InputGenerator<AdressebeskyttelseInput> {
    override suspend fun generer(input: RegelInput): AdressebeskyttelseInput {
        val personer = requireNotNull(
            pdlService.hentPersonBolk(
                input.søkerIdenter.søker.union(input.søkerIdenter.barn).toList(),
                input.callId
            )
        )
        return AdressebeskyttelseInput(input.roller, personer)
    }

}