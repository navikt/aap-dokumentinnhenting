package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.brev.BrevGateway
import java.time.LocalDate
import java.time.LocalDateTime

class DialogmeldingBrevGeneratorService(
    private val brevGateway: BrevGateway,
) {
    suspend fun genererMedSignatur(
        personNavn: String,
        personIdent: String,
        behandlerNavn: String?,
        behandlerHprNr: String?,
        dialogmeldingTekst: String,
        dokumentasjonType: DokumentasjonType,
        tidligereBestillingDato: LocalDateTime?,
        saksnummer: String?,
        bestillerNavIdent: String,
    ): String {
        val signatur = brevGateway.hentSignaturForhåndsvisning(personIdent, bestillerNavIdent)
        return genererDialogmelding(
            BrevGenerering(
                personNavn = personNavn,
                personIdent = personIdent,
                behandlerNavn = behandlerNavn,
                behandlerHprNr = behandlerHprNr,
                dialogmeldingTekst = dialogmeldingTekst,
                dokumentasjonType = dokumentasjonType,
                forsendelseDato = LocalDate.now(),
                tidligereBestillingDato = tidligereBestillingDato,
                saksnummer = saksnummer,
                signatur = signatur,
            )
        )
    }
}
