package dokumentinnhenting.integrasjoner.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Vedlegg

data class VarselOmBrevbestillingDto(
    val behandlingsReferanse: BehandlingReferanse,
    val vedlegg: Vedlegg
)