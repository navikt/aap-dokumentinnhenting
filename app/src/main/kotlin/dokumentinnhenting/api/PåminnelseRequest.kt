package dokumentinnhenting.api

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.Rolle
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.util.UUID

data class PåminnelseRequest(
    val dialogmeldingUuid: UUID,
    val behandlingsreferanse: UUID
) : Behandlingsreferanse {
    override fun behandlingsreferanseResolverInput(): String {
        return behandlingsreferanse.toString()
    }

    override fun hentPåkrevdRolle(): List<Rolle> {
        return Definisjon.BESTILL_LEGEERKLÆRING.løsesAv
    }
}