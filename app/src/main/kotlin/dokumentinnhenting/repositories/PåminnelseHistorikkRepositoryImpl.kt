package dokumentinnhenting.repositories

import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseHistorikkRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

class PåminnelseHistorikkRepositoryImpl(private val connection: DBConnection) : PåminnelseHistorikkRepository {

    override fun lagreNyPåminnelseStatusEndring(påminnelse: PåminnelseRecord, opprettetAvIdent: Bruker, endringTidspunkt: LocalDateTime) {
        val query = """
            INSERT INTO PAAMINNELSE_HISTORIKK(
                PAAMINNELSE_ID, OPPRETTET_AV, PAAMINNELSE_STATUS, HENDELSE_TIDSPUNKT
            )
            SELECT ?, ?, ?, ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, påminnelse.id)
                setString(2, opprettetAvIdent.ident)
                setString(3, påminnelse.status.name)
                setLocalDateTime(4, endringTidspunkt)
            }
        }
    }

    override fun hentPåminnelserPåPåminnelseId(påminnelseId: Long): List<PåminnelseHistorikkRecord> {
        val query = """
            SELECT * FROM PAAMINNELSE_HISTORIKK
            WHERE PAAMINNELSE_ID = ?
            ORDER BY HENDELSE_TIDSPUNKT
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, påminnelseId)
            }
            setRowMapper(::mapPåminnelseHistorikkRecord)
        }
    }

    private fun mapPåminnelseHistorikkRecord(row: Row): PåminnelseHistorikkRecord =
        PåminnelseHistorikkRecord(
            id = row.getLong("ID"),
            påminnelseId = row.getLong("PAAMINNELSE_ID"),
            opprettetAv = Bruker(row.getString("OPPRETTET_AV")),
            påminnelseStatus = row.getEnum("PAAMINNELSE_STATUS"),
            hendelseTidspunkt = row.getLocalDateTime("HENDELSE_TIDSPUNKT")
        )
}