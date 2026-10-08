package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.tilFellesDialogmeldingDto
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

class DialogmeldingUthentingService(
    private val dataSource: DataSource,
) {
    fun hentFellesDialogmeldingerForSak(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = hentSendteDialogmeldinger(saksnummer)
        val mottatteDialogmeldinger = hentMottatteDialogmeldinger(saksnummer)

        return sendteDialogmeldinger + mottatteDialogmeldinger
    }

    private fun hentSendteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val sendteDialogmeldinger = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentForSaksnummer(saksnummer)
        }

        return sendteDialogmeldinger.map { it.tilFellesDialogmeldingDto() }
    }

    private fun hentMottatteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val mottatteDialogmeldinger = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSaksnummer(saksnummer)
        }

        return mottatteDialogmeldinger.map { it.tilFellesDialogmeldingDto() }
    }

    fun hentLegeerklæringForespørslerForSak(behandlingsReferanse: BehandlingReferanse): List<FellesDialogmeldingDto> {
        val legeerklæringForespørsler = dataSource.transaction { connection ->
            DialogmeldingRepository(connection).hentBestillingerForDokumentasjonstyper(
                behandlingsReferanse,
                listOf(DokumentasjonType.L40)
            )
        }

        return legeerklæringForespørsler.map { it.tilFellesDialogmeldingDto()}
    }
}