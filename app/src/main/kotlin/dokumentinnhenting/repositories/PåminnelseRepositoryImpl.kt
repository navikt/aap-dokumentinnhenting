package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.repository.Repository
import java.util.UUID

class PåminnelseRepositoryImpl(private val connection: DBConnection) : PåminnelseRepository {

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
                WHERE DIALOGMELDING_UUID = ANY(?::text[])
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
            påminnelseDato = row.getLocalDateTime("PAMINNELSESDATO"),
            opprettetTidspunkt = row.getLocalDateTime("OPPRETTET_TIDSPUNKT"),
            sisteForespørselSendt = row.getLocalDateTimeOrNull("FORESPORSEL_SENDT"),
        )
    }
}