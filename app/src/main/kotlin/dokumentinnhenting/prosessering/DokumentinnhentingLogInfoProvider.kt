package dokumentinnhenting.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.LogInformasjon

object DokumentinnhentingLogInfoProvider : JobbLogInfoProvider {

    override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon? {

        /*
        * Henter info til jobb med dialogmeldingUuid / msgId
        * Se extension-funksjon JobbInput.medDialogmeldingUuid()
        */
        val dialogmeldingUuid = jobbInput.dialogmeldingUuidOrNull() ?: return null

        val dialogmeldingMap = connection.queryFirstOrNull(
            "SELECT dialogmelding_uuid, journalpost_id FROM dialogmelding WHERE dialogmelding_uuid = ?"
        ) {
            setParams {
                setUUID(1, dialogmeldingUuid)
            }
            setRowMapper { row ->
                buildMap {
                    row.getStringOrNull("dialogmelding_uuid")?.let { put("dialogmeldingUuid", it) }
                    row.getStringOrNull("journalpost_id")?.let { put("journalpostId", it) }
                }
            }
        }

        val mottattDialogmeldingMap = connection.queryFirstOrNull(
            "SELECT msg_id, journalpost_id FROM mottatt_dialogmelding WHERE msg_id = ?"
        ) {
            setParams {
                setUUID(1, dialogmeldingUuid)
            }
            setRowMapper { row ->
                buildMap {
                    row.getStringOrNull("msg_id")?.let { put("dialogmeldingUuid", it) }
                    row.getStringOrNull("journalpost_id")?.let { put("journalpostId", it) }
                }
            }
        }

        return if (dialogmeldingMap == null && mottattDialogmeldingMap == null) null
        else LogInformasjon((dialogmeldingMap ?: emptyMap()) + (mottattDialogmeldingMap ?: emptyMap()))
    }
}
