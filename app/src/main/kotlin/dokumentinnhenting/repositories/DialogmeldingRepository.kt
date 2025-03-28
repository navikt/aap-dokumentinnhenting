package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusDTO
import dokumentinnhenting.integrasjoner.syfo.status.DialogmeldingStatusTilBehandslingsflytDTO
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.util.motor.syfo.ProsesseringSyfoStatus
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import java.time.LocalDateTime
import java.util.*

class DialogmeldingRepository(private val connection: DBConnection) {
    fun opprettDialogmelding(melding: DialogmeldingRecord): UUID {
        val query = """
            INSERT INTO DIALOGMELDING (
                dialogmelding_uuid, behandler_ref, person_id, person_navn, saksnummer, 
                dokumentasjontype, behandler_navn, fritekst, 
                behandlingsReferanse, tidligere_bestilling_referanse, behandler_hpr_nr, bestiller_nav_ident
                )
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        connection.executeReturnKey(query) {
            setParams {
                setUUID(1, melding.dialogmeldingUuid)
                setString(2, melding.behandlerRef)
                setString(3, melding.personIdent)
                setString(4, melding.personNavn)
                setString(5, melding.saksnummer)
                setString(6, melding.dokumentasjonType.toString())
                setString(7, melding.behandlerNavn)
                setString(8, melding.fritekst)
                setUUID(9, melding.behandlingsReferanse)
                setUUID(10, melding.tidligereBestillingReferanse)
                setString(11, melding.behandlerHprNr)
                setString(12, melding.bestillerNavIdent)
            }
        }
        return melding.dialogmeldingUuid
    }

    fun oppdaterDialogmeldingStatus(melding: DialogmeldingStatusDTO) {
        val query = """
            UPDATE DIALOGMELDING
            SET STATUS = ?, STATUS_TEKST = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setString(1, melding.status.toString())
                setString(2, melding.tekst)
                setUUID(3, UUID.fromString(melding.bestillingUuid))
            }
        }
    }

    fun oppdaterDialogmeldingStatusMedMottatt(dialogmeldingUuid: UUID) {
        val query = """
            UPDATE DIALOGMELDING
            SET STATUS = ?, STATUS_TEKST = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.executeReturnUpdated(query) {
            setParams {
                setString(1, MeldingStatusType.MOTTATT.toString())
                setString(2, MeldingStatusType.MOTTATT.toString())
                setUUID(3, dialogmeldingUuid)
            }
        }
    }

    fun leggTilJournalpostPåBestilling(dialogmeldingUuid: UUID, journalpostId: String, dokumentId: String) {
        val query = """
            UPDATE DIALOGMELDING
            SET JOURNALPOST_ID = ?, DOKUMENT_ID = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setString(1, journalpostId)
                setString(2, dokumentId)
                setUUID(3, dialogmeldingUuid)
            }
        }
    }

    fun oppdaterFlytStatus(dialogmeldingUuid: UUID, flytStatus: ProsesseringSyfoStatus) {
        val query = """
            UPDATE DIALOGMELDING
            SET FLYTSTATUS = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setString(1, flytStatus.toString())
                setUUID(2, dialogmeldingUuid)
            }
        }
    }

    fun hentalleDialogIder(): List<String> {
        val query = """
            SELECT DIALOGMELDING_UUID FROM DIALOGMELDING
        """.trimIndent()

        return connection.queryList(query) {
            setRowMapper { it.getString("DIALOGMELDING_UUID") }
        }
    }

    fun hentSisteBestillingByPIDYngreEnn2mMnd(personId: String): String? {
        val query = """
            SELECT SAKSNUMMER FROM DIALOGMELDING
            WHERE OPPRETTET_TID > NOW() - INTERVAL '2 months' AND PERSON_ID = ?
            ORDER BY OPPRETTET_TID DESC LIMIT 1
        """.trimIndent()

        return connection.queryFirstOrNull(query){
            setParams {
                setString(1, personId)
            }
            setRowMapper {
                it.getString("SAKSNUMMER")
            }
        }
    }

    fun hentBestillingEldreEnn14Dager(dialogmeldingUuid: UUID): DialogmeldingFullRecord? {
        val query = """
            SELECT * FROM DIALOGMELDING
            WHERE OPPRETTET_TID < NOW() - INTERVAL '14 days' AND DIALOGMELDING_UUID = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query){
            setParams {
                setUUID(1, dialogmeldingUuid)
            }
            setRowMapper(::mapDialogmeldingFullRecord)
        }
    }

    fun hentBySaksnummer(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDTO> {
        val query = """
            SELECT * FROM DIALOGMELDING
            WHERE SAKSNUMMER = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setString(1, saksnummer)
            }
            setRowMapper {
                DialogmeldingStatusTilBehandslingsflytDTO(
                    it.getUUID("DIALOGMELDING_UUID"),
                    it.getEnumOrNull("STATUS"),
                    it.getStringOrNull("STATUS_TEKST"),
                    it.getString("BEHANDLER_REF"),
                    it.getString("BEHANDLER_NAVN"),
                    it.getString("PERSON_ID"),
                    it.getString("SAKSNUMMER"),
                    it.getLocalDateTime("OPPRETTET_TID"),
                    it.getUUID("BEHANDLINGSREFERANSE"),
                    it.getString("FRITEKST")
                )
            }
        }
    }

    fun hentByDialogId(dialogmeldingUuid: UUID): DialogmeldingFullRecord? {
        val query = """
            SELECT * FROM DIALOGMELDING
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setUUID(1, dialogmeldingUuid)
            }
            setRowMapper(::mapDialogmeldingFullRecord)
        }
    }

    fun hentFlytStatus(dialogmeldingUuid: UUID): SyfoBestillingFlytStatus {
        val query = """
            SELECT * FROM DIALOGMELDING
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, dialogmeldingUuid)
            }
            setRowMapper {
                SyfoBestillingFlytStatus(
                    it.getUUID("DIALOGMELDING_UUID"),
                    it.getString("SAKSNUMMER"),
                    it.getEnumOrNull("FLYTSTATUS")
                )
            }
        }
    }

    fun låsBestilling(dialogmeldingUuid: UUID): UUID {
        val query = """SELECT DIALOGMELDING_UUID FROM DIALOGMELDING WHERE DIALOGMELDING_UUID = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, dialogmeldingUuid)
            }
            setRowMapper {
                it.getUUID("DIALOGMELDING_UUID")
            }
        }
    }

    private fun mapDialogmeldingFullRecord(row: Row): DialogmeldingFullRecord {
        return DialogmeldingFullRecord(
            bestillerNavIdent = row.getString("BESTILLER_NAV_IDENT"),
            dialogmeldingUuid = row.getUUID("DIALOGMELDING_UUID"),
            behandlerRef = row.getString("BEHANDLER_REF"),
            behandlerNavn = row.getString("BEHANDLER_NAVN"),
            behandlerHprNr = row.getString("BEHANDLER_HPR_NR"),
            personIdent = row.getString("PERSON_ID"),
            dokumentasjonType = row.getEnum("DOKUMENTASJONTYPE"),
            fritekst = row.getString("FRITEKST"),
            saksnummer = row.getString("SAKSNUMMER"),
            status = row.getEnumOrNull("STATUS"),
            flytStatus = row.getEnumOrNull("FLYTSTATUS"),
            personNavn = row.getString("PERSON_NAVN"),
            statusTekst = row.getStringOrNull("STATUS_TEKST"),
            behandlingsReferanse = row.getUUID("BEHANDLINGSREFERANSE"),
            opprettet = row.getLocalDateTime("OPPRETTET_TID"),
            tidligereBestillingReferanse = row.getUUIDOrNull("TIDLIGERE_BESTILLING_REFERANSE"),
            journalpostId = row.getStringOrNull("JOURNALPOST_ID"),
            dokumentId = row.getStringOrNull("DOKUMENT_ID"),
            id = row.getLong("ID")
        )
    }

    data class SyfoBestillingFlytStatus(
        val dialogmeldingUuid: UUID,
        val saksnummer: String,
        val flytStatus: ProsesseringSyfoStatus?,
    )
}