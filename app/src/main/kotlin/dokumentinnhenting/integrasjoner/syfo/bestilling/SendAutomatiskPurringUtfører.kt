package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger(SendAutomatiskPurringUtfører::class.java)

class SendAutomatiskPurringUtfører(
    private val bestillingService: BehandlerDialogmeldingBestillingService,
    private val behandlingsflytGateway: BehandlingsflytGateway
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val kjøredatoForJobb = input.opprettetTidspunkt().toLocalDate()
        val bestillingOpprettetDatoForPåminnelse = if (Miljø.erProd()) {
            kjøredatoForJobb.minusWeeks(3).minusDays(1)
        } else {
            kjøredatoForJobb.minusDays(1)
        }

        val kandidater =
            behandlingsflytGateway.finnKandidaterForAutomatiskPåminnelse(bestillingDatoForPåminnelse = bestillingOpprettetDatoForPåminnelse)
        log.info(
            "Fikk ${kandidater.size} kandidater for påminnelse fra behandlingsflyt: ${
                kandidater.map { it.referanse }.joinToString(", ")
            }"
        )
        // skru på bare i dev foreløpig
        if (Miljø.erProd()) {
            return
        }
        kandidater.forEach {
            bestillingService.sendAutomatiskPåminnelseHvisBestillingFinnes(
                it,
                bestillingOpprettetDato = bestillingOpprettetDatoForPåminnelse
            )
        }
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendAutomatiskPurringUtfører(
                bestillingService = BehandlerDialogmeldingBestillingService.konstruer(connection),
                behandlingsflytGateway = BehandlingsflytGateway
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