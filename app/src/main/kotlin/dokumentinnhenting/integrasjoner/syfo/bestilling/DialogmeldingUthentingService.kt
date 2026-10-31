package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.tilFellesDialogmeldingDto
import dokumentinnhenting.api.tilPåminnelseRecord
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import dokumentinnhenting.repositories.PåminnelseRepository
import dokumentinnhenting.unleash.FeatureToggles
import dokumentinnhenting.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import java.time.LocalDateTime
import java.util.UUID

class DialogmeldingUthentingService(
    val dialogmeldingRepository: DialogmeldingRepositoryImpl,
    val mottattDialogmeldingRepository: MottattDialogmeldingRepository,
    val påminnelseRepository: PåminnelseRepository,
    val unleash: UnleashGateway
) {
    fun hentFellesDialogmeldingerForSak(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = hentSendteDialogmeldinger(saksnummer)
        val mottatteDialogmeldinger = hentMottatteDialogmeldinger(saksnummer)

        return sendteDialogmeldinger + mottatteDialogmeldinger
    }

    private fun hentSendteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = dialogmeldingRepository.hentForSaksnummer(saksnummer)

        if (unleash.isEnabled(FeatureToggles.KnappForManuellPaaminnelse)) {
            leggTilManglendePåminnelser(
                sendteDialogmeldinger.filter { it.dokumentasjonType == DokumentasjonType.L40 }
            )

            val dialogmeldingIDerSomKanPåminnes = utledKanSendeManuellPåminnelseMedDato(sendteDialogmeldinger)

            return sendteDialogmeldinger.map {
                it.tilFellesDialogmeldingDto(dialogmeldingIDerSomKanPåminnes)
            }
        }

        return sendteDialogmeldinger.map { it.tilFellesDialogmeldingDto(emptyMap()) }
    }

    private fun hentMottatteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val mottatteDialogmeldinger = mottattDialogmeldingRepository.hentForSaksnummer(saksnummer)

        return mottatteDialogmeldinger.map { it.tilFellesDialogmeldingDto() }
    }

    fun hentLegeerklæringForespørslerForSakUtenSjekkManuellPåminnelse(
        behandlingsReferanse: BehandlingReferanse
    ): List<FellesDialogmeldingDto> {
        val legeerklæringForespørsler = dialogmeldingRepository.hentBestillingerForDokumentasjonstyper(
            behandlingsReferanse,
            listOf(DokumentasjonType.L40)
        )

        return legeerklæringForespørsler.map { it.tilFellesDialogmeldingDto(emptyMap<UUID, LocalDateTime>()) }
    }

    private fun utledKanSendeManuellPåminnelseMedDato(
        dialogmeldingForespørsler: List<DialogmeldingFullRecord>
    ): Map<UUID, LocalDateTime?> {
        val påminnelser = påminnelseRepository.hentPåminnelseForDialogmeldinger(
            dialogmeldingForespørsler.map { it.dialogmeldingUuid }
        )

        // Antagelse 1: At man ikke kan sende manuell forespørsel før fristen for automatisk påminnelse har passert,
        // enten automatisk påminnelse har blitt sendt eller ikke.
        // Antagelse 2: At det opprettes en automatisk påminnelse for hver forespørsel om L40.
        // Antagelse 3: Maks ett påminnelseobjekt per dialogmelding/forespørsel på L40.
        return påminnelser
            .asSequence()
            .filter { påminnelse ->
                påminnelse.harPåminnelseDatoPassert() &&
                !påminnelse.erForespørselSendtSisteAntallDager(MINSTE_ANTALL_DØGN_SIDEN_FORRIGE_PÅMINNELSE)
            }
            .associate { it.dialogmeldingId to it.sisteForespørselSendt }
    }

    // TODO 2522: Bruk av denne ved henting kan fjernes i fremtiden hvis vi setter et tak på hvor lenge man kan purre
    //  etter at en forespørsel er sendt, f.eks. 5 uker, slik at vi kan være sikre på at alle eksisterende og aktive
    //  forespørsler om L40 vil ha et påminnelseobjekt ved opprettelse.
    // TODO 2522: Alternativt å lage en jobb som oppretter Påminnelserader for de L40-forespørslene som mangler dette,
    //  da kan i så fall denne droppes mye tidligere.
    private fun leggTilManglendePåminnelser(
        legeerklæringForespørsler: List<DialogmeldingFullRecord>
    ) {
        val eksisterendePåminnelserUUIDer = påminnelseRepository.hentPåminnelseForDialogmeldinger(
            legeerklæringForespørsler.map { it.dialogmeldingUuid }
        ).associateBy { it.dialogmeldingId }

        val forespørslerUtenPåminnelseRad = legeerklæringForespørsler
            .filter { !eksisterendePåminnelserUUIDer.contains(it.dialogmeldingUuid) }

        val nyePåminnelser = forespørslerUtenPåminnelseRad.map { legeerklæring ->
            legeerklæring.tilPåminnelseRecord()
        }

        påminnelseRepository.opprettPåminnelseListeEllerOppdaterStatus(nyePåminnelser)
    }
}