package dokumentinnhenting.util

import com.papsign.ktor.openapigen.APITag

enum class Tags(override val description: String) : APITag {
    Dokumenter("Endepunkter for uthenting og behandling av dokumenter i SAF / Dokarkiv"),
    Påminnelse("Endepunkter relatert til påminnelser."),
    Dialogmelding("Endepunkter relatert til dialogmelding."),
    Syfo(""),
    Test("Endepunkter som kun skal være tilgjengelig i test.")
}
