package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.pdl.Ident
import dokumentinnhenting.integrasjoner.pdl.PdlIdentGateway
import dokumentinnhenting.integrasjoner.pdl.PersonIdentGateway
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.prosessering.medDialogmeldingUuid
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.DialogmeldingRepositoryImpl
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRepositoryImpl
import dokumentinnhenting.util.metrics.DialogmeldingFiltreringsutfall
import dokumentinnhenting.util.metrics.prometheus
import dokumentinnhenting.util.metrics.recordDialogmeldingFiltrert
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory
import java.util.UUID

class FiltrerDialogmeldingUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val dialogmeldingRepository: DialogmeldingRepository,
    private val mottattDialogmeldingRepository: MottattDialogmeldingRepository,
    private val personIdentGateway: PersonIdentGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val payload: DialogmeldingMottakDTO =
            DefaultJsonMapper.fromJson<DialogmeldingMottakDTO>(input.payload())

        if (Miljø.erDev() && payload.journalpostId == "0") {
            // Skal kun skje i testmiljøet. Vil være "0" i tilfeller hvor Syfo ikke klarer å opprette journalpost.
            log.warn("Håndterer ikke dialogmelding fordi journalpostId er 0.")
            prometheus.recordDialogmeldingFiltrert(DialogmeldingFiltreringsutfall.UGYLDIG_JOURNALPOST)
            return
        }

        val sakstilknytning = finnSaksnummer(payload)

        if (sakstilknytning != null) {
            opprettJobb(payload, sakstilknytning.saksnummer, skalLagreMottatDialogmelding = true)
            prometheus.recordDialogmeldingFiltrert(sakstilknytning.utfall)
            return
        }

        if (payload.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null) {
            log.info("Fant ikke kobling fra mottatt til sendt dialogmelding. Henter saksinfo fra behandlingsflyt for dialogmelding med journalpostId ${payload.journalpostId}")
            val saksInfo = BehandlingsflytGateway.finnÅpenSakForIdentPåDato(
                payload.personIdentPasient,
                payload.mottattTidspunkt.toLocalDate()
            )

            if (saksInfo == null) {
                log.info("Fant ikke åpen sak for personident på dialogmelding med journalpostId ${payload.journalpostId}")
                prometheus.recordDialogmeldingFiltrert(DialogmeldingFiltreringsutfall.INGEN_SAK)
                return
            } else {
                log.info("Fant åpen sak for dialogmelding. msgId: ${payload.msgId}, conversationRef: ${payload.conversationRef}, parentRef: ${payload.parentRef}, journalpostId: ${payload.journalpostId}")
                opprettJobb(payload, saksInfo.saksnummer, skalLagreMottatDialogmelding = false)
                prometheus.recordDialogmeldingFiltrert(DialogmeldingFiltreringsutfall.BEHANDLINGSFLYT)
            }
        } else {
            prometheus.recordDialogmeldingFiltrert(DialogmeldingFiltreringsutfall.MANGLER_FORESPORSELSSVAR)
        }
    }

    private data class Sakstilknytning(val saksnummer: String, val utfall: DialogmeldingFiltreringsutfall)

    private fun finnSaksnummer(dialogmelding: DialogmeldingMottakDTO): Sakstilknytning? {
        val personIdenter =
            personIdentGateway.hentAlleIdenterForPerson(Ident(dialogmelding.personIdentPasient))
                .map { it.identifikator }
                .also {
                    check(it.isNotEmpty()) { "PDL returnerte ingen folkeregisteridenter" }
                }

        return finnSaksnummerViaSendtDialogmelding(dialogmelding, personIdenter)
    }

    private fun finnSaksnummerViaSendtDialogmelding(
        mottattDialogmelding: DialogmeldingMottakDTO, personIdenter: List<String>
    ): Sakstilknytning? {
        return mottattDialogmelding.conversationRef?.toUUIDOrNull()
            ?.let {
                dialogmeldingRepository.hentForSamtale(
                    samtaleRef = it,
                    personIdenter = personIdenter
                )
            }
            .orEmpty()
            .map { it.saksnummer }
            .distinct()
            .also { saksnumre ->
                check(saksnumre.size <= 1) {
                    "Fant flere saksnumre for samme samtale i sendte dialogmeldinger"
                }
            }
            .singleOrNull()
            ?.also { log.info("Fant kobling fra mottatt til sendt dialogmelding basert på conversationRef. msgId: ${mottattDialogmelding.msgId}") }
            ?.let { Sakstilknytning(it, DialogmeldingFiltreringsutfall.SAMTALE) }
            ?: mottattDialogmelding.parentRef?.toUUIDOrNull()
                ?.let {
                    dialogmeldingRepository.hentForParent(
                        parentRef = it,
                        personIdenter = personIdenter
                    )
                }
                ?.saksnummer
                ?.also { log.info("Fant kobling fra mottatt til sendt dialogmelding basert på parentRef. msgId: ${mottattDialogmelding.msgId}") }
                ?.let { Sakstilknytning(it, DialogmeldingFiltreringsutfall.PARENT) }
    }

    private fun opprettJobb(
        mottattDialogmelding: DialogmeldingMottakDTO,
        saksnummer: String,
        skalLagreMottatDialogmelding: Boolean,
    ) {
        flytJobbRepository.leggTil(
            JobbInput(HåndterMottattDialogmeldingUtfører).medPayload(
                DefaultJsonMapper.toJson(
                    FiltrertDialogmeldingMedSakstilknytning(
                        skalLagreMottattDialogmelding = skalLagreMottatDialogmelding,
                        dialogmeldingMottatt = mottattDialogmelding,
                        sakOgBehandling = BehandlingsflytGateway.SakOgBehandling(saksnummer),
                    )
                )
            ).medDialogmeldingUuid(mottattDialogmelding.msgId)
        )
    }

    fun String.toUUIDOrNull(): UUID? {
        return runCatching {
            UUID.fromString(this)
        }.getOrNull()
    }

    companion object : Jobb {
        override fun beskrivelse(): String {
            return "Ansvarlig for å filtrere dialogmeldinger som vi har sak på"
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FiltrerDialogmeldingUtfører(
                flytJobbRepository = FlytJobbRepository(connection),
                dialogmeldingRepository = DialogmeldingRepositoryImpl(connection),
                mottattDialogmeldingRepository = MottattDialogmeldingRepositoryImpl(connection),
                personIdentGateway = PdlIdentGateway(),
            )
        }

        override fun navn(): String {
            return "Filtererer mottatt dialogmelding"
        }

        override fun type(): String {
            return "dialogmelding.filter"
        }
    }
}
