package dokumentinnhenting.integrasjoner.behandlingsflyt

import dokumentinnhenting.Fakes
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.Vedlegg
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BehandlingsflytGatewayTest {
    val fakes = Fakes

    @Test
    fun `får bestilt varsel med request mot behandlingsflyt`() {
        BehandlingsflytGateway.sendVarslingsbrev(
            VarselOmBrevbestillingDto(
                BehandlingReferanse(UUID.randomUUID()),
                UUID.randomUUID(),
                Vedlegg("", "")
            )
        )
    }
}
