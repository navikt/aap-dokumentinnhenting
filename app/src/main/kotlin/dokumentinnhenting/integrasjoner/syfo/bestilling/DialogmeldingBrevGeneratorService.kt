package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.brev.BrevGateway
import java.time.LocalDateTime

class DialogmeldingBrevGeneratorService(
    private val brevGateway: BrevGateway,
) {
    suspend fun genererMedSignatur(
        personNavn: String,
        personIdent: String,
        dialogmeldingTekst: String,
        dokumentasjonType: DokumentasjonType,
        tidligereBestillingDato: LocalDateTime?,
        bestillerNavIdent: String,
    ): String {
        val signatur = brevGateway.hentSignaturForhåndsvisning(personIdent, bestillerNavIdent)
        return genererDialogmelding(
            BrevGenerering(
                personNavn = personNavn,
                personIdent = personIdent,
                dialogmeldingTekst = dialogmeldingTekst,
                dokumentasjonType = dokumentasjonType,
                tidligereBestillingDato = tidligereBestillingDato,
                signatur = signatur,
            )
        )
    }
}
