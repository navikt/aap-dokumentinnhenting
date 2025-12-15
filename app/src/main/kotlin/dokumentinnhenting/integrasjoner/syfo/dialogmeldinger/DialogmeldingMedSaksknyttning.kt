package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO

data class DialogmeldingMedSaksknyttning(
    val dialogmeldingMottatt: DialogmeldingMottakDTO,
    val sakOgBehandling: BehandlingsflytGateway.SakOgBehandling
)
