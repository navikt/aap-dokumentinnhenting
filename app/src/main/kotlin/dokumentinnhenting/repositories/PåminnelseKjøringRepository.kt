package dokumentinnhenting.repositories

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class PåminnelseKjøringRepository(private val connection: DBConnection) {
    fun hentSistKjørtForDato(): LocalDate? {
        val sistKjørtForDato = """
            SELECT bestillingsdato FROM paaminnelse_kjoering
            ORDER BY bestillingsdato DESC
            LIMIT 1
        """.trimIndent()

        return connection.queryFirstOrNull(sistKjørtForDato) {
            setRowMapper {
                it.getLocalDate("bestillingsdato")
            }
        }
    }

    fun lagreKjøringForDato(dato: LocalDate) {
        val lagreKjøring = """
            INSERT INTO paaminnelse_kjoering (kjoert_dato, bestillingsdato)
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(lagreKjøring) {
            setParams {
                setLocalDate(1, LocalDate.now())
                setLocalDate(2, dato)
            }
        }

    }
}