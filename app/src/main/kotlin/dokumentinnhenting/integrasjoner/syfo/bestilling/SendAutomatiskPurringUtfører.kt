package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import dokumentinnhenting.repositories.PåminnelseKjøringRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.LocalDate


private val log = LoggerFactory.getLogger(SendAutomatiskPurringUtfører::class.java)

private val dagerÅTrekkeFra = if (Miljø.erProd()) 22L else 0L

class SendAutomatiskPurringUtfører(
    private val bestillingService: BehandlerDialogmeldingBestillingService,
    private val behandlingsflytGateway: BehandlingsflytGateway,
    private val påminnelseKjøringRepository: PåminnelseKjøringRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val sistKjørtForBestillingsdato = påminnelseKjøringRepository.hentSistKjørtForDato()
        val datoerSomMåSendesPåminnelseFor = if (sistKjørtForBestillingsdato == null) {
            log.info("Ingen tidligere kjøring funnet, sender påminnelse for bestillinger opprettet for tre uker og en dag siden.")
            listOf(LocalDate.now().minusDays(dagerÅTrekkeFra))
        } else {
            log.info(
                "Sist kjørt for bestillingsdato: $sistKjørtForBestillingsdato, sender påminnelse for alle bestillinger opprettet mellom $sistKjørtForBestillingsdato og ${
                    LocalDate.now().minusDays(dagerÅTrekkeFra)
                }"
            )
            val startDatoForKjøring = sistKjørtForBestillingsdato.plusDays(1)
            val sluttDatoForKjøring = LocalDate.now().minusDays(dagerÅTrekkeFra)
            startDatoForKjøring.datesUntil(sluttDatoForKjøring.plusDays(1))
                .toList()
        }
        datoerSomMåSendesPåminnelseFor.forEach { bestillingOpprettetDatoForPåminnelse ->
            val kandidater =
                behandlingsflytGateway.finnKandidaterForAutomatiskPåminnelse(bestillingDatoForPåminnelse = bestillingOpprettetDatoForPåminnelse)
            log.info(
                "Fikk ${kandidater.size} kandidater for påminnelse fra behandlingsflyt for dato $bestillingOpprettetDatoForPåminnelse: ${
                    kandidater.map { it.referanse }.joinToString(", ")
                }"
            )

            // ikke send eller lagre noe i prod inntil videre
            if (Miljø.erProd()) {
                return@forEach
            }
            kandidater.forEach {
                bestillingService.sendAutomatiskPåminnelseHvisBestillingFinnes(
                    it,
                    bestillingOpprettetDato = bestillingOpprettetDatoForPåminnelse
                )
            }
            påminnelseKjøringRepository.lagreKjøringForDato(bestillingOpprettetDatoForPåminnelse)
        }
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendAutomatiskPurringUtfører(
                bestillingService = BehandlerDialogmeldingBestillingService.konstruer(connection),
                behandlingsflytGateway = BehandlingsflytGateway,
                påminnelseKjøringRepository = PåminnelseKjøringRepository(connection)
            )
        }

        override fun type(): String {
            return "sendAutomatiskPurring"
        }

        override fun navn(): String {
            return "Sender automatisk purring på legeerklæring etter tre uker."
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å finne behandlinger med bestilling som skal purres på og sende purringen."
        }

        override val cron = CronExpression.create("0 0 8 * * *")
    }
}