package dokumentinnhenting.integrasjoner.syfo.status

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.OffsetDateTime
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.Serdes

data class DialogmeldingStatusDto(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val status: MeldingStatusType,
    val tekst: String?,
    val bestillingUuid: String,
)

/*
* MOTTATT er ikke en status som eksisterer i ISYFO, men noe vi setter selv når vi mottar legeerklæring via postmottak
* */
enum class MeldingStatusType {
    BESTILT, SENDT, OK, AVVIST, MOTTATT
}

private class DialogmeldingStatusDTOSerializer : Serializer<DialogmeldingStatusDto> {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

    override fun serialize(topic: String?, data: DialogmeldingStatusDto?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

private class DialogmeldingStatusDTODeserializer : Deserializer<DialogmeldingStatusDto> {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

    override fun deserialize(topic: String?, data: ByteArray?): DialogmeldingStatusDto? {
        return data?.let { objectMapper.readValue(it, object : TypeReference<DialogmeldingStatusDto>() {}) }
    }
}

fun dialogmeldingStatusDTOSerde(): Serde<DialogmeldingStatusDto> {
    return Serdes.serdeFrom(DialogmeldingStatusDTOSerializer(), DialogmeldingStatusDTODeserializer())
}