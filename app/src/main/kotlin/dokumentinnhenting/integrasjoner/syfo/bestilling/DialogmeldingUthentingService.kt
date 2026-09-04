package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.api.mapLeveringStatus
import dokumentinnhenting.api.tilDto
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående
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

        return sendteDialogmeldinger.map { dialogmelding ->
            FellesDialogmeldingDto(
                innkommendeUtgående = InnkommendeUtgående.UTGÅENDE,
                meldingFraNavn = dialogmelding.behandlerNavn,
                opprettetTidspunkt = dialogmelding.opprettet,
                dokumentasjonsType = dialogmelding.dokumentasjonType.tilDto(),
                tekst = dialogmelding.fritekst,
                meldingStatus = dialogmelding.status?.mapLeveringStatus(),
                journalpostId = dialogmelding.journalpostId
            )
        }
    }

    private fun hentMottatteDialogmeldinger(saksnummer: String): List<FellesDialogmeldingDto> {
        val mottatteDialogmeldinger = dataSource.transaction { connection ->
            MottattDialogmeldingRepository(connection).hentForSaksnummer(saksnummer)
        }

        return mottatteDialogmeldinger.map { dialogmelding ->
            FellesDialogmeldingDto(
                innkommendeUtgående = InnkommendeUtgående.INNKOMMENDE,
                meldingFraNavn = dialogmelding.navnHelsepersonell,
                opprettetTidspunkt = dialogmelding.opprettetTid,
                dokumentasjonsType = null,
                tekst = dialogmelding.tekstNotatInnhold,
                meldingStatus = null,
                journalpostId = dialogmelding.journalpostId
            )
        }
    }
}