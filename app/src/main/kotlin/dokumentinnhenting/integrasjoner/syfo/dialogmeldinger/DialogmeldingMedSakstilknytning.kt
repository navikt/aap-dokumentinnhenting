package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO

data class FiltrertDialogmeldingMedSakstilknytning(
    val skalLagreMottattDialogmelding: Boolean = false,
    val dialogmeldingMottatt: DialogmeldingMottakDTO,
    val saksnummer: String
)

data class DialogmeldingMedSakstilknytning(
    val dialogmeldingMottatt: DialogmeldingMottakDTO,
    val saksnummer: String
)
