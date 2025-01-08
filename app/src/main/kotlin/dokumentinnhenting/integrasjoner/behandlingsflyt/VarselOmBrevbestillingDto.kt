package dokumentinnhenting.integrasjoner.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Vedlegg
import java.util.UUID

data class VarselOmBrevbestillingDto(
    val behandlingsReferanse: BehandlingReferanse,
    val dialogmeldingUUID: UUID,
    val vedlegg: Vedlegg
)