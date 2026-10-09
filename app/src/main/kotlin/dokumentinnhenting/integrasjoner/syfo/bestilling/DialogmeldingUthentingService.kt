package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.tilFellesDialogmeldingDto
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto

class DialogmeldingUthentingService(
    val dialogmeldingRepository: DialogmeldingRepositoryImpl,
    val mottattDialogmeldingRepository: MottattDialogmeldingRepository
) {
    fun hentFellesDialogmeldingerForSak(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = hentSendteDialogmeldinger(saksnummer)
        val mottatteDialogmeldinger = hentMottatteDialogmeldinger(saksnummer)

        return sendteDialogmeldinger + mottatteDialogmeldinger
    }

    private fun hentSendteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = dialogmeldingRepository.hentForSaksnummer(saksnummer)

        return sendteDialogmeldinger.map { it.tilFellesDialogmeldingDto() }
    }

    private fun hentMottatteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val mottatteDialogmeldinger = mottattDialogmeldingRepository.hentForSaksnummer(saksnummer)

        return mottatteDialogmeldinger.map { it.tilFellesDialogmeldingDto() }
    }

    fun hentLegeerklæringForespørslerForSak(behandlingsReferanse: BehandlingReferanse): List<FellesDialogmeldingDto> {
        val legeerklæringForespørsler = dialogmeldingRepository.hentBestillingerForDokumentasjonstyper(
            behandlingsReferanse,
            listOf(DokumentasjonType.L40)
        )

        return legeerklæringForespørsler.map { it.tilFellesDialogmeldingDto() }
    }
}