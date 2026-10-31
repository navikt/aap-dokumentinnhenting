package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class PåminnelseRepositoryImpl(private val connection: DBConnection) : PåminnelseRepository {

    override fun opprettPåminnelseEllerOppdaterStatus(påminnelse: PåminnelseRecord) {
        val query = """
            INSERT INTO PAAMINNELSE (
                DIALOGMELDING_UUID, BEHANDLINGSREFERANSE, STATUS, PAMINNELSESDATO, OPPRETTET_TIDSPUNKT, SISTE_FORESPORSEL_SENDT
            )
            SELECT ?, ?, ?, ?, ?, ?
            ON CONFLICT (DIALOGMELDING_UUID) DO UPDATE SET 
                STATUS = ?
        """.trimIndent()

        return connection.execute(query) {
            setParams {
                setUUID(1, påminnelse.dialogmeldingId)
                setUUID(2, påminnelse.behandlingsReferanse)
                setString(3, påminnelse.status.toString())
                setLocalDate(4, påminnelse.påminnelseDato)
                setLocalDateTime(5, påminnelse.opprettetTidspunkt.truncatedTo(ChronoUnit.MILLIS))
                setLocalDateTime(6, påminnelse.sisteForespørselSendt)
                setString(7, påminnelse.status.toString())
            }
        }
    }

    override fun opprettPåminnelseListeEllerOppdaterStatus(påminnelser: List<PåminnelseRecord>) {
        if (påminnelser.isEmpty()) {
            return
        }

        val query = """
            INSERT INTO PAAMINNELSE (
                DIALOGMELDING_UUID, BEHANDLINGSREFERANSE, STATUS, 
                PAMINNELSESDATO, OPPRETTET_TIDSPUNKT, SISTE_FORESPORSEL_SENDT
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (DIALOGMELDING_UUID) DO UPDATE SET
                STATUS = ?
        """.trimIndent()

        connection.executeBatch(query, påminnelser) {
            setParams { påminnelse ->
                setUUID(1, påminnelse.dialogmeldingId)
                setUUID(2, påminnelse.behandlingsReferanse)
                setString(3, påminnelse.status.toString())
                setLocalDate(4, påminnelse.påminnelseDato)
                setLocalDateTime(5, påminnelse.opprettetTidspunkt.truncatedTo(ChronoUnit.MILLIS))
                setLocalDateTime(6, påminnelse.sisteForespørselSendt)
                setString(7, påminnelse.status.toString())
            }
        }
    }

    override fun hentPåminnelseForId(påminnelseId: Long): PåminnelseRecord? {
        val query = """
            SELECT * FROM PAAMINNELSE
            WHERE Id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, påminnelseId)
            }
            setRowMapper {
                mapPåminnelseRecord(it)
            }
        }
    }

    override fun hentPåminnelseForDialogmeldingId(dialogmeldingId: UUID): List<PåminnelseRecord> {
        val query = """
            SELECT * FROM PAAMINNELSE
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setUUID(1, dialogmeldingId)
            }
            setRowMapper {
                mapPåminnelseRecord(it)
            }
        }
    }

    override fun hentPåminnelseForDialogmeldinger(dialogmeldingIdListe: List<UUID>): List<PåminnelseRecord> {
        val query = """
            SELECT * FROM PAAMINNELSE
                WHERE DIALOGMELDING_UUID = ANY(?::UUID[])
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setArray(1, dialogmeldingIdListe.map { it.toString() })
            }
            setRowMapper {
                mapPåminnelseRecord(it)
            }
        }
    }

    override fun hentPåminnelseForBehandlingsReferanse(behandlingsReferanse: BehandlingReferanse): List<PåminnelseRecord> {
        val query = """
            SELECT * FROM PAAMINNELSE
            WHERE BEHANDLINGSREFERANSE = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setUUID(1, behandlingsReferanse.referanse)
            }
            setRowMapper {
                mapPåminnelseRecord(it)
            }
        }
    }

    override fun oppdaterPåminnelseStatus(dialogmeldingId: UUID, status: PåminnelseStatus) {
        val query = """
            UPDATE PAAMINNELSE
            SET STATUS = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setUUID(2, dialogmeldingId)
            }
        }
    }

    override fun oppdaterPåminnelseDato(dialogmeldingId: UUID, dato: LocalDate) {
        val query = """
            UPDATE PAAMINNELSE
            SET PAMINNELSESDATO = ?
            WHERE DIALOGMELDING_UUID = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLocalDate(1, dato)
                setUUID(2, dialogmeldingId)
            }
        }
    }

    private fun mapPåminnelseRecord(row: Row): PåminnelseRecord {
        return PåminnelseRecord(
            id = row.getLong("ID"),
            dialogmeldingId = row.getUUID("DIALOGMELDING_UUID"),
            behandlingsReferanse = row.getUUID("BEHANDLINGSREFERANSE"),
            status = row.getEnum("STATUS"),
            påminnelseDato = row.getLocalDate("PAMINNELSESDATO"),
            opprettetTidspunkt = row.getLocalDateTime("OPPRETTET_TIDSPUNKT"),
            sisteForespørselSendt = row.getLocalDateTimeOrNull("SISTE_FORESPORSEL_SENDT"),
        )
    }
}