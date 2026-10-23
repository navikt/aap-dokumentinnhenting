package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.repository.Repository
import java.time.temporal.ChronoUnit
import java.util.UUID

class PåminnelseRepositoryImpl(private val connection: DBConnection) : PåminnelseRepository {

    override fun opprettPåminnelse(påminnelse: PåminnelseRecord): Long {
        val query = """
            INSERT INTO PAAMINNELSE (
                DIALOGMELDING_UUID, BEHANDLINGSREFERANSE, STATUS, PAMINNELSESDATO, OPPRETTET_TIDSPUNKT, SISTE_FORESPORSEL_SENDT
            )
            VALUES (
                ?, ?, ?, ?, ?, ?
            )
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setUUID(1, påminnelse.dialogmeldingId)
                setUUID(2, påminnelse.behandlingsReferanse)
                setString(3, påminnelse.status.toString())
                setLocalDate(4, påminnelse.påminnelseDato)
                setLocalDateTime(5, påminnelse.opprettetTidspunkt.truncatedTo(ChronoUnit.MILLIS))
                setLocalDateTime(6, påminnelse.sisteForespørselSendt)
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