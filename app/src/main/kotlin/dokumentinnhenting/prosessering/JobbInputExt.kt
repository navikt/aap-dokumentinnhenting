package dokumentinnhenting.prosessering

import no.nav.aap.motor.JobbInput

private const val DIALOGMELDING_UUID_KEY = "dialogmeldingUuid"

/**
 * Hack siden jobb/motor er satt opp til å kun støtte sakId/behandlingId (Long) som ikke gir mening her
 */
fun JobbInput.medDialogmeldingUuid(uuid: Any?): JobbInput =
    this.apply {
        uuid?.let { dialogmeldingUuid ->
            medParameter(DIALOGMELDING_UUID_KEY, dialogmeldingUuid.toString())
        }
    }

fun JobbInput.dialogmeldingUuidOrNull(): String? =
    this.optionalParameter(DIALOGMELDING_UUID_KEY)
