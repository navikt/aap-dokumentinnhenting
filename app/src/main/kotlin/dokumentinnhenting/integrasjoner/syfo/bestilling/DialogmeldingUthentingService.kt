package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.tilFellesDialogmeldingDto
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import dokumentinnhenting.repositories.PåminnelseRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import java.time.LocalDateTime
import java.util.UUID

const val MINSTE_ANTALL_DØGN_SIDEN_FORRIGE_PÅMINNELSE = 1L

class DialogmeldingUthentingService(
    val dialogmeldingRepository: DialogmeldingRepositoryImpl,
    val mottattDialogmeldingRepository: MottattDialogmeldingRepository,
    val påminnelseRepository: PåminnelseRepository
) {
    fun hentFellesDialogmeldingerForSak(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = hentSendteDialogmeldinger(saksnummer)
        val mottatteDialogmeldinger = hentMottatteDialogmeldinger(saksnummer)

        return sendteDialogmeldinger + mottatteDialogmeldinger
    }

    private fun hentSendteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = dialogmeldingRepository.hentForSaksnummer(saksnummer)
        val dialogmeldingIDerSomKanPåminnes = utledKanSendeManuellPåminnelseMedDato(sendteDialogmeldinger)

        return sendteDialogmeldinger.map {
            it.tilFellesDialogmeldingDto(dialogmeldingIDerSomKanPåminnes)
        }
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
        val forespørslerIDerSomKanPåminnes = utledKanSendeManuellPåminnelseMedDato(legeerklæringForespørsler)

        return legeerklæringForespørsler.map { it.tilFellesDialogmeldingDto(forespørslerIDerSomKanPåminnes) }
    }

    private fun utledKanSendeManuellPåminnelseMedDato(dialogmeldinger: List<DialogmeldingFullRecord>): Map<UUID, LocalDateTime?> {
        val påminnelser = påminnelseRepository.hentPåminnelseForDialogmeldinger(
            dialogmeldinger.map { it.dialogmeldingUuid }
        )

        return påminnelser
            .asSequence()
            .filter { påminnelse ->
                påminnelse.status == PåminnelseStatus.SENDT &&
                påminnelse.sisteForespørselSendt != null &&
                påminnelse.sisteForespørselSendt.plusDays(MINSTE_ANTALL_DØGN_SIDEN_FORRIGE_PÅMINNELSE) < LocalDateTime.now()
            }
            .associate { it.dialogmeldingId to it.sisteForespørselSendt }
    }
}