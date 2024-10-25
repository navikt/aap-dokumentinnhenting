package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.util.*

class DialogmeldingRepository(private val connection: DBConnection) {
    fun opprettDialogmelding(melding: DialogmeldingRecord): UUID {
        val query = """
            INSERT INTO DIALOGMELDING (dialogmelding_uuid, behandler_ref, person_id, sak_id)
                 VALUES (?, ?, ?, ?)
            """.trimIndent()
        connection.executeReturnKey(query) {
            setParams {
                setUUID(1, melding.dialogmeldingUuid)
                setString(2, melding.behandlerRef)
                setString(3, melding.personIdent)
                setString(4, melding.sakId)
            }
        }
        return melding.dialogmeldingUuid
    }

    fun oppdaterDialogmeldingStatus(melding: DialogmeldingStatusDTO) {
        val query = """
            UPDATE DIALOGMELDING
            SET STATUS = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setString(1, melding.status.toString())
                setUUID(2, UUID.fromString(melding.bestillingUuid))
            }
        }
    }

    fun hentBySakId(sakId: String): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        val query = """
            SELECT * FROM DIALOGMELDING
            WHERE SAK_ID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setString(1, sakId)
            }
            setRowMapper {
                DialogmeldingStatusTilBehandslingsflytDTO(
                    it.getUUID("DIALOGMELDING_UUID"),
                    it.getEnumOrNull("STATUS"),
                    it.getString("BEHANDLER_REF"),
                    it.getString("PERSON_ID"),
                    it.getString("SAK_ID")
                )
            }
        }
    }
}