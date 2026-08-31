package no.nav.aap.dokumentinnhenting.kontrakt

public data class AvsenderMottakerDto(
    val id: String?,
    val type: AvsenderMottakerIdType?,
    val navn: String?,
) {
    public enum class AvsenderMottakerIdType {
        FNR,
        ORGNR,
        HPRNR,
        UTL_ORG,
        NULL,
        UKJENT,
    }
}