package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

/**
 * Innkommende dialogmelding fra fastleger og andre behandlere.
 *
 * @see: https://github.com/navikt/padm2/blob/master/documentation/kafka/dialogmelding.md
 **/
@JsonIgnoreProperties(ignoreUnknown = true)
data class DialogmeldingMottakDTO(
    val msgId: String,
    val msgType: String,
    val mottattTidspunkt: LocalDateTime,
    val conversationRef: String?,
    val parentRef: String?,
    val personIdentPasient: String,
    val legehpr: String?,
    val dialogmelding: Dialogmelding,
    val journalpostId: String,
)

data class Dialogmelding(
    val id: String,
    val innkallingMoterespons: InnkallingMoterespons?,
    val foresporselFraSaksbehandlerForesporselSvar: ForesporselFraSaksbehandlerForesporselSvar?,
    val henvendelseFraLegeHenvendelse: HenvendelseFraLegeHenvendelse?,
    val navnHelsepersonell: String,
    val signaturDato: LocalDateTime
)

data class HenvendelseFraLegeHenvendelse(
    val temaKode: TemaKode,
    val tekstNotatInnhold: String,
    val dokIdNotat: String?,
    val foresporsel: Foresporsel?,
    val rollerRelatertNotat: RollerRelatertNotat?
)

data class InnkallingMoterespons(
    val temaKode: TemaKode?,
    val tekstNotatInnhold: String?,
    val dokIdNotat: String?,
    val foresporsel: Foresporsel?
)

data class TemaKode(
    val kodeverkOID: String,
    val dn: String,
    val v: String,
    val arenaNotatKategori: String,
    val arenaNotatKode: String,
    val arenaNotatTittel: String
)

data class ForesporselFraSaksbehandlerForesporselSvar(
    val temaKode: TemaKode,
    val tekstNotatInnhold: String,
    val dokIdNotat: String?,
    val datoNotat: LocalDateTime?
)

data class Foresporsel(
    val typeForesp: TypeForesp,
    val sporsmal: String,
    val dokIdForesp: String?,
    val rollerRelatertNotat: RollerRelatertNotat?
)

data class RollerRelatertNotat(
    val rolleNotat: RolleNotat?,
    val person: Person?,
    val helsepersonell: Helsepersonell?
)

data class Helsepersonell(
    val givenName: String,
    val familyName: String
)

data class Person(
    val givenName: String,
    val familyName: String
)

data class RolleNotat(
    val s: String,
    val v: String
)

data class TypeForesp(
    val dn: String,
    val s: String,
    val v: String
)
