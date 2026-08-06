package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak.DialogmeldingMottakDTO
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.komponenter.dbconnect.DBConnection

class MottattDialogmeldingRepository(private val connection: DBConnection) {

    fun lagre(dialogmelding: DialogmeldingMottakDTO, saksnummer: String) {
        val query = """
            INSERT INTO MOTTATT_DIALOGMELDING (
                msg_id, msg_type, mottatt_tidspunkt, conversation_ref, parent_ref, 
                person_ident_pasient, lege_hpr, journalpost_id, navn_helsepersonell, 
                tekst_notat_innhold, dialogmelding_type, dialogmelding_dn, saksnummer, opprettet_tid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val dialogmeldingDetaljer = dialogmelding.dialogmeldingDetaljer()

        connection.execute(query) {
            setParams {
                setUUID(1, UUID.fromString(dialogmelding.msgId))
                setString(2, dialogmelding.msgType)
                setLocalDateTime(3, dialogmelding.mottattTidspunkt)
                setUUID(4, dialogmelding.conversationRef?.toUUIDOrNull())
                setUUID(5, dialogmelding.parentRef?.toUUIDOrNull())
                setString(6, dialogmelding.personIdentPasient)
                setString(7, dialogmelding.legehpr)
                setString(8, dialogmelding.journalpostId)
                setString(9, dialogmelding.dialogmelding.navnHelsepersonell)
                setString(10, dialogmeldingDetaljer.tekstNotatInnhold)
                setEnumName(11, dialogmeldingDetaljer.dialogmeldingType)
                setString(12, dialogmeldingDetaljer.dn)
                setString(13, saksnummer)
                setLocalDateTime(14, LocalDateTime.now())
            }
        }
    }

    fun eksisterer(dialogmeldingId: UUID): Boolean {
        val query = "SELECT EXISTS(SELECT 1 FROM MOTTATT_DIALOGMELDING WHERE MSG_ID = ?)"

        return connection.queryFirst(query) {
            setParams { setUUID(1, dialogmeldingId) }
            setRowMapper { it.getBoolean("exists") }
        }
    }

    internal fun hentForMsgId(msgId: UUID): MottattDialogmeldingRecord? {
        val query = "SELECT * FROM MOTTATT_DIALOGMELDING WHERE MSG_ID = ?"

        return connection.queryFirstOrNull(query) {
            setParams { setUUID(1, msgId) }
            setRowMapper { row ->
                MottattDialogmeldingRecord(
                    id = row.getLong("ID"),
                    msgId = row.getUUID("MSG_ID"),
                    msgType = row.getString("MSG_TYPE"),
                    mottattTidspunkt = row.getLocalDateTime("MOTTATT_TIDSPUNKT"),
                    conversationRef = row.getUUIDOrNull("CONVERSATION_REF"),
                    parentRef = row.getUUIDOrNull("PARENT_REF"),
                    personIdentPasient = row.getString("PERSON_IDENT_PASIENT"),
                    legehpr = row.getStringOrNull("LEGE_HPR"),
                    navnHelsepersonell = row.getString("NAVN_HELSEPERSONELL"),
                    dialogmeldingType = row.getEnumOrNull("DIALOGMELDING_TYPE"),
                    tekstNotatInnhold = row.getStringOrNull("TEKST_NOTAT_INNHOLD"),
                    dn = row.getStringOrNull("DIALOGMELDING_DN"),
                    journalpostId = row.getString("JOURNALPOST_ID"),
                    saksnummer = row.getString("SAKSNUMMER"),
                    opprettetTid = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }
    }

    private fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private fun DialogmeldingMottakDTO.dialogmeldingDetaljer(): MottattDialogmeldingDetaljer {
        return when {
            dialogmelding.foresporselFraSaksbehandlerForesporselSvar != null -> {
                MottattDialogmeldingDetaljer(
                    dialogmeldingType = DialogmeldingType.FORESPORSEL_SVAR,
                    tekstNotatInnhold = dialogmelding.foresporselFraSaksbehandlerForesporselSvar.tekstNotatInnhold,
                    dn = dialogmelding.foresporselFraSaksbehandlerForesporselSvar.temaKode.dn,
                )
            }

            dialogmelding.henvendelseFraLegeHenvendelse != null -> {
                MottattDialogmeldingDetaljer(
                    dialogmeldingType = DialogmeldingType.HENVENDELSE,
                    tekstNotatInnhold = dialogmelding.henvendelseFraLegeHenvendelse.tekstNotatInnhold,
                    dn = dialogmelding.henvendelseFraLegeHenvendelse.temaKode.dn,
                )
            }

            dialogmelding.innkallingMoterespons != null -> {
                MottattDialogmeldingDetaljer(
                    dialogmeldingType = DialogmeldingType.MOTEINNKALLING_SVAR,
                    tekstNotatInnhold = dialogmelding.innkallingMoterespons.tekstNotatInnhold,
                    dn = dialogmelding.innkallingMoterespons.temaKode?.dn,
                )
            }

            else -> {
                throw IllegalArgumentException("Ukjent type dialogmelding for dialogmelding med id ${dialogmelding.id}")
            }
        }
    }
}

internal data class MottattDialogmeldingRecord(
    val id: Long,
    val msgId: UUID,
    val msgType: String,
    val mottattTidspunkt: LocalDateTime,
    val conversationRef: UUID?,
    val parentRef: UUID?,
    val personIdentPasient: String,
    val legehpr: String?,
    val navnHelsepersonell: String,
    val dialogmeldingType: DialogmeldingType?,
    val tekstNotatInnhold: String?,
    val dn: String?,
    val journalpostId: String,
    val saksnummer: String,
    val opprettetTid: LocalDateTime,
)

internal enum class DialogmeldingType {
    FORESPORSEL_SVAR,
    HENVENDELSE,
    MOTEINNKALLING_SVAR,
}

private data class MottattDialogmeldingDetaljer(
    val dialogmeldingType: DialogmeldingType,
    val tekstNotatInnhold: String?,
    val dn: String?,
)
