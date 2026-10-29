package dokumentinnhenting.repositories

import java.time.LocalDate

interface PåminnelseKjøringRepository {
    fun hentSistKjørtForDato(): LocalDate?
    fun lagreKjøringForDato(dato: LocalDate)
}