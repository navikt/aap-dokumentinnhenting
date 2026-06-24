package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.Fakes.behandlingsflytSakResponses
import dokumentinnhenting.WithFakes
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.Dialogmelding
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.ForesporselFraSaksbehandlerForesporselSvar
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.TemaKode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FiltrerDialogmeldingUtførerTest {

    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val utfører = FiltrerDialogmeldingUtfører(flytJobbRepository)

    @AfterEach
    fun tearDown() {
        behandlingsflytSakResponses.clear()
    }

    @Test
    fun `skal legge til ny jobb når sak finnes, journalpostId er gyldig og svar er satt`() {
        val dto = lagDialogmeldingMottakDTO()
        behandlingsflytSakResponses[dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate()] =
            randomUUID().toString()
        every { flytJobbRepository.leggTil(any()) } just runs

        utfører.utfør(lagJobbInput(dto))

        verify(exactly = 1) { flytJobbRepository.leggTil(match { it.payload().contains(dto.personIdentPasient) }) }
    }

    @Test
    fun `skal ikke legge til jobb når ingen åpen sak finnes`() {
        val dto = lagDialogmeldingMottakDTO()
        behandlingsflytSakResponses.remove(dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate())

        utfører.utfør(lagJobbInput(dto))

        verify(exactly = 0) { flytJobbRepository.leggTil(match { it.payload().contains(dto.personIdentPasient) }) }
    }

    @Test
    fun `skal ikke legge til jobb når journalpostId er 0`() {
        val dto = lagDialogmeldingMottakDTO(journalpostId = "0")
        behandlingsflytSakResponses[dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate()] =
            randomUUID().toString()

        utfører.utfør(lagJobbInput(dto))

        verify(exactly = 0) { flytJobbRepository.leggTil(match { it.payload().contains(dto.personIdentPasient) }) }
    }

    @Test
    fun `skal ikke legge til jobb når foresporselSvar er null`() {
        val dto = lagDialogmeldingMottakDTO(foresporselSvar = null)
        behandlingsflytSakResponses[dto.personIdentPasient to dto.mottattTidspunkt.toLocalDate()] =
            randomUUID().toString()

        utfører.utfør(lagJobbInput(dto))

        verify(exactly = 0) { flytJobbRepository.leggTil(match { it.payload().contains(dto.personIdentPasient) }) }
    }

    private fun lagJobbInput(dto: DialogmeldingMottakDTO): JobbInput =
        JobbInput(FiltrerDialogmeldingUtfører).medPayload(DefaultJsonMapper.toJson(dto))

    private fun lagDialogmeldingMottakDTO(
        journalpostId: String = randomUUID().toString(),
        foresporselSvar: ForesporselFraSaksbehandlerForesporselSvar? = lagForesporselSvar(),
    ) = DialogmeldingMottakDTO(
        msgId = randomUUID().toString(),
        msgType = randomUUID().toString(),
        navLogId = randomUUID().toString(),
        mottattTidspunkt = LocalDateTime.now(),
        conversationRef = null,
        parentRef = null,
        personIdentPasient = Random.nextLong(10000000000, 99999999999).toString(),
        personIdentBehandler = Random.nextLong(10000000000, 99999999999).toString(),
        legekontorOrgNr = null,
        legekontorHerId = null,
        legekontorOrgName = randomUUID().toString(),
        legehpr = null,
        dialogmelding = Dialogmelding(
            id = randomUUID().toString(),
            innkallingMoterespons = null,
            foresporselFraSaksbehandlerForesporselSvar = foresporselSvar,
            henvendelseFraLegeHenvendelse = null,
            navnHelsepersonell = randomUUID().toString(),
            signaturDato = LocalDateTime.now(),
        ),
        antallVedlegg = 0,
        journalpostId = journalpostId,
        fellesformatXML = randomUUID().toString(),
    )

    private fun lagForesporselSvar() = ForesporselFraSaksbehandlerForesporselSvar(
        temaKode = TemaKode(
            kodeverkOID = randomUUID().toString(),
            dn = randomUUID().toString(),
            v = randomUUID().toString(),
            arenaNotatKategori = randomUUID().toString(),
            arenaNotatKode = randomUUID().toString(),
            arenaNotatTittel = randomUUID().toString(),
        ),
        tekstNotatInnhold = randomUUID().toString(),
        dokIdNotat = null,
        datoNotat = null,
    )
}
