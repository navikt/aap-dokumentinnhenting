package dokumentinnhenting.integrasjoner.syfo.dialogmeldinger

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.integrasjoner.pdl.Ident
import dokumentinnhenting.integrasjoner.pdl.PdlIdentGateway
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import dokumentinnhenting.prosessering.medDialogmeldingUuid
import dokumentinnhenting.repositories.DialogmeldingRepository
import dokumentinnhenting.repositories.MottattDialogmeldingRecord
import dokumentinnhenting.repositories.MottattDialogmeldingRepository
import dokumentinnhenting.unleash.FeatureToggles
import dokumentinnhenting.unleash.UnleashGateway
import dokumentinnhenting.unleash.UnleashGatewayImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
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
    private val unleash: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val payload: DialogmeldingMottakDTO =
            DefaultJsonMapper.fromJson<DialogmeldingMottakDTO>(input.payload())

        if (Miljø.erDev() && payload.journalpostId == "0") {
            // Skal kun skje i testmiljøet. Vil være "0" i tilfeller hvor Syfo ikke klarer å opprette journalpost.
            log.warn("Håndterer ikke dialogmelding fordi journalpostId er 0.")
            return
        }

        val personIdentMelding = payload.personIdentPasient
        val saksnummer = finnSaksnummer(payload, personIdentMelding)

        if (saksnummer != null) {
            opprettJobb(payload, saksnummer, skalLagreMottatDialogmelding = true)
            return
        }

        if (payload.dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null) {
            log.info("Fant ikke kobling fra mottatt til sendt dialogmelding. Henter saksinfo fra behandlingsflyt for dialogmelding med journalpostId ${payload.journalpostId}")
            val saksInfo = BehandlingsflytGateway.finnÅpenSakForIdentPåDato(
                personIdentMelding,
                payload.mottattTidspunkt.toLocalDate()
            )

            if (saksInfo == null) {
                log.info("Fant ikke åpen sak for personident på dialogmelding med journalpostId ${payload.journalpostId}")
                return
            } else {
                log.info("Fant åpen sak for dialogmelding. msgId: ${payload.msgId}, conversationRef: ${payload.conversationRef}, parentRef: ${payload.parentRef}, journalpostId: ${payload.journalpostId}")
                opprettJobb(payload, saksInfo.saksnummer, skalLagreMottatDialogmelding = false)
            }
        }
    }

    private fun finnSaksnummerPåIdentViaDialogmelding(dialogmelding: DialogmeldingMottakDTO, personIdent: String): String? {
        return finnKoblingViaSendtDialogmelding(dialogmelding, personIdent)?.saksnummer
            ?: finnKoblingViaTidligereMottattDialogmelding(dialogmelding, personIdent)?.saksnummer
    }

    private fun finnSaksnummerGjennomIdentHistorikk(dialogmelding: DialogmeldingMottakDTO): String? {
        val personIdentMelding = dialogmelding.personIdentPasient
        val identHistorikk = PdlIdentGateway().hentAlleIdenterForPerson(Ident(personIdentMelding))

        identHistorikk.filter {it.identifikator != personIdentMelding}.forEach {
            val saksnummer = finnSaksnummerPåIdentViaDialogmelding(dialogmelding, it.identifikator)
            if (saksnummer != null) {
                oppdaterIdentPåDialogmeldinger(identHistorikk, dialogmelding)
                return saksnummer
            }
        }

        return null
    }

    private fun finnSaksnummer(payload: DialogmeldingMottakDTO, personIdentMelding: String): String? {
        val saksnummer = finnSaksnummerPåIdentViaDialogmelding(payload, personIdentMelding)

        if (!unleash.isEnabled(FeatureToggles.HaandterEndringAvIdentIDialogmelding)) {
            return saksnummer
        }

        return saksnummer ?: finnSaksnummerGjennomIdentHistorikk(payload)
    }

    private fun finnKoblingViaSendtDialogmelding(
        mottattDialogmelding: DialogmeldingMottakDTO, personIdent: String
    ): DialogmeldingFullRecord? {
        return mottattDialogmelding.conversationRef?.toUUIDOrNull()
            ?.let {
                dialogmeldingRepository.hentForSamtale(
                    samtaleRef = it,
                    personIdent = personIdent
                )
            }
            ?.maxByOrNull { it.opprettet }
            ?.also { log.info("Fant kobling fra mottatt til sendt dialogmelding basert på conversationRef. msgId: ${mottattDialogmelding.msgId}") }
            ?: mottattDialogmelding.parentRef?.toUUIDOrNull()
                ?.let {
                    dialogmeldingRepository.hentForParent(
                        parentRef = it,
                        personIdent = personIdent
                    )
                }
                ?.also { log.info("Fant kobling fra mottatt til sendt dialogmelding basert på parentRef. msgId: ${mottattDialogmelding.msgId}") }
    }

    // Midlertidig kobling med logging med tidligere mottatt melding som kan ha blitt koblet med parentRef til utgående melding.
    // Dette siden vi ikke har full historikk på conversationRef på utgående meldinger. Denne mappingen bør ikke treffe
    // etterhvert som vi har conversationRef på alle utestående forespøsler/utgående dialogmeldinger.
    private fun finnKoblingViaTidligereMottattDialogmelding(
        mottattDialogmelding: DialogmeldingMottakDTO, personIdent: String
    ): MottattDialogmeldingRecord? {
        return mottattDialogmelding.conversationRef?.toUUIDOrNull()
            ?.let { conversationRef ->
                mottattDialogmeldingRepository
                    .hentForSamtale(conversationRef, personIdent)
                    .firstOrNull()
            }
            ?.also { log.info("Fant kobling fra mottatt til tidligere mottatt dialogmelding basert på conversationRef. msgId: ${mottattDialogmelding.msgId}") }
    }

    private fun oppdaterIdentPåDialogmeldinger(identHistorikk: List<Ident>, dialogmelding: DialogmeldingMottakDTO) {
        val aktivIdent = identHistorikk.firstOrNull { it.aktivIdent }?.identifikator
        if (aktivIdent == null) {
            throw VerdiIkkeFunnetException("Det fantes ingen aktiv ident i identhistorikken!")
        }

        dialogmelding.conversationRef?.toUUIDOrNull()
            ?.let { conversationRef ->
                dialogmeldingRepository.oppdaterPersonIdentPåSamtaleRef(
                    conversationRef,
                    identHistorikk.map { it.identifikator },
                    aktivIdent
                )
                mottattDialogmeldingRepository.oppdaterPersonIdentPåSamtaleRef(
                    conversationRef,
                    identHistorikk.map { it.identifikator },
                    aktivIdent
                )
            }
        dialogmelding.parentRef?.toUUIDOrNull()
            ?.let { parentRef ->
                dialogmeldingRepository.oppdaterPersonIdentPåParentRef(
                    parentRef,
                    identHistorikk.map { it.identifikator },
                    aktivIdent
                )
                mottattDialogmeldingRepository.oppdaterPersonIdentPåParentRef(
                    parentRef,
                    identHistorikk.map { it.identifikator },
                    aktivIdent
                )
            }
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
                dialogmeldingRepository = DialogmeldingRepository(connection),
                mottattDialogmeldingRepository = MottattDialogmeldingRepository(connection),
                unleash = UnleashGatewayImpl
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
