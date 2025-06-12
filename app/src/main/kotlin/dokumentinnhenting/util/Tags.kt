package dokumentinnhenting.util

import com.papsign.ktor.openapigen.APITag

enum class Tags(override val description: String) : APITag {
    Dokumenter("Endepunkter for uthenting og behandling av dokumenter i SAF / Dokarkiv"),
}
