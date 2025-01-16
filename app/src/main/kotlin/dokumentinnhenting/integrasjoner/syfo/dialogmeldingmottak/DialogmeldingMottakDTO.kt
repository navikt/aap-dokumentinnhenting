package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import java.time.LocalDateTime

data class DialogmeldingMottakDTO(
    val msgId: String,
    val msgType: String,
    val navLogId: String,
    val mottattTidspunkt: LocalDateTime,
    val conversationRef: String?,
    val parentRef: String?,
    val personIdentPasient: String,
    val pasientAktoerId: String? = null, // deprecated
    val personIdentBehandler: String,
    val behandlerAktoerId: String? = null, // deprecated
    val legekontorOrgNr: String?,
    val legekontorHerId: String?,
    val legekontorReshId: String? = null, // deprecated
    val legekontorOrgName: String,
    val legehpr: String?,
    val dialogmelding: Dialogmelding,
    val antallVedlegg: Int,
    val journalpostId: String,
    val fellesformatXML: String,
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

private class DialogmeldingMottakDTOSerializer : Serializer<DialogmeldingMottakDTO> {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    override fun serialize(topic: String?, data: DialogmeldingMottakDTO?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

private class DialogmeldingMottakDTODeserializer : Deserializer<DialogmeldingMottakDTO> {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    override fun deserialize(topic: String?, data: ByteArray?): DialogmeldingMottakDTO? {
        return data?.let { objectMapper.readValue(it, object : TypeReference<DialogmeldingMottakDTO>() {}) }
    }
}

fun dialogmeldingMottakDTOSerde(): Serde<DialogmeldingMottakDTO> {
    return Serdes.serdeFrom(DialogmeldingMottakDTOSerializer(), DialogmeldingMottakDTODeserializer())
}