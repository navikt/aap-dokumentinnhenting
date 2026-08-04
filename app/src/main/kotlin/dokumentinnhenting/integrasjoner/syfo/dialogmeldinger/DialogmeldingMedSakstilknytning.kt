package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO

data class FiltrertDialogmeldingMedSakstilknytning(
    val skalLagreMottattDialogmelding: Boolean = false,
    val dialogmeldingMottatt: DialogmeldingMottakDTO,
    val sakOgBehandling: BehandlingsflytGateway.SakOgBehandling
)

data class DialogmeldingMedSakstilknytning(
    val dialogmeldingMottatt: DialogmeldingMottakDTO,
    val sakOgBehandling: BehandlingsflytGateway.SakOgBehandling
)
