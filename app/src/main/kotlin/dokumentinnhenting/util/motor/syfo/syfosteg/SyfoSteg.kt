package dokumentinnhenting.util.motor.syfo.syfosteg

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.util.*

sealed interface SyfoSteg {

    fun konstruer(connection: DBConnection): Utfører

    sealed interface Utfører {
        fun utfør(kontekst: Kontekst): Resultat
    }

    data class Kontekst(val referanse: UUID)

    enum class Resultat {
        FULLFØRT, STOPP
    }
}