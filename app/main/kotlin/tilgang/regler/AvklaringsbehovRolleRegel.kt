package tilgang.regler

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon as PostmottakDefinisjon
import tilgang.Rolle

data object AvklaringsbehovRolleRegel : Regel<AvklaringsbehovRolleInput> {
    override fun vurder(input: AvklaringsbehovRolleInput): Boolean {
        return kanAvklareBehov(input.avklaringsbehov, input.roller)
    }

    private fun kanAvklareBehov(avklaringsbehov: Definisjon, roller: List<Rolle>): Boolean {
        val erLokalSaksbehandler = erLokalSaksbehandler(roller)
        val erNaySaksbehandler = erNaySaksbehandler(roller)
        val erBeslutter = erBeslutter(roller)

        return when (avklaringsbehov) {
            Definisjon.MANUELT_SATT_PÅ_VENT -> erLokalSaksbehandler || erNaySaksbehandler
            Definisjon.AVKLAR_STUDENT -> erNaySaksbehandler
            Definisjon.AVKLAR_SYKDOM -> erLokalSaksbehandler
            Definisjon.AVKLAR_BISTANDSBEHOV -> erLokalSaksbehandler
            Definisjon.FRITAK_MELDEPLIKT -> erLokalSaksbehandler
            Definisjon.FASTSETT_ARBEIDSEVNE -> erLokalSaksbehandler
            Definisjon.AVKLAR_SYKEPENGEERSTATNING -> erNaySaksbehandler
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT -> erNaySaksbehandler
            Definisjon.AVKLAR_BARNETILLEGG -> erNaySaksbehandler
            Definisjon.AVKLAR_SONINGSFORRHOLD -> erNaySaksbehandler
            Definisjon.AVKLAR_HELSEINSTITUSJON -> erNaySaksbehandler
            Definisjon.AVKLAR_SAMORDNING_GRADERING -> erNaySaksbehandler
            Definisjon.FORESLÅ_VEDTAK -> erNaySaksbehandler
            Definisjon.KVALITETSSIKRING -> erNaySaksbehandler
            Definisjon.FATTE_VEDTAK -> erBeslutter
        }
    }
    
    private fun kanAvklareBehov(avklaringsbehov: PostmottakDefinisjon, roller: List<Rolle>): Boolean {
        return when (avklaringsbehov) {
            else -> erNaySaksbehandler(roller)
        }
    }

    private fun erLokalSaksbehandler(roller: List<Rolle>): Boolean {
        return Rolle.VEILEDER in roller
    }

    private fun erNaySaksbehandler(roller: List<Rolle>): Boolean {
        return Rolle.SAKSBEHANDLER in roller
    }

    private fun erBeslutter(roller: List<Rolle>): Boolean {
        return Rolle.BESLUTTER in roller
    }
}

data class AvklaringsbehovRolleInput(val avklaringsbehov: Definisjon, val roller: List<Rolle>)

data object AvklaringsbehovInputGenerator : InputGenerator<AvklaringsbehovRolleInput> {
    override suspend fun generer(input: RegelInput): AvklaringsbehovRolleInput {
        val avklaringsbehov =
            requireNotNull(input.avklaringsbehovFraBehandlingsflyt) { "Avklaringsbehov er påkrevd" }
        return AvklaringsbehovRolleInput(avklaringsbehov, input.roller)
    }
}