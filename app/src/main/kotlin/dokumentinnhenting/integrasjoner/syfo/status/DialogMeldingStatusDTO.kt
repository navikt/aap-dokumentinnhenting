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

data class DialogmeldingStatusDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val status: MeldingStatusType,
    val tekst: String?,
    val bestillingUuid: String,
)

enum class MeldingStatusType {
    BESTILT, SENDT, OK, AVVIST
}

private class DialogmeldingStatusDTOSerializer : Serializer<DialogmeldingStatusDTO> {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

    override fun serialize(topic: String?, data: DialogmeldingStatusDTO?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

private class DialogmeldingStatusDTODeserializer : Deserializer<DialogmeldingStatusDTO> {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

    override fun deserialize(topic: String?, data: ByteArray?): DialogmeldingStatusDTO? {
        return data?.let { objectMapper.readValue(it, object : TypeReference<DialogmeldingStatusDTO>() {}) }
    }
}

fun dialogmeldingStatusDTOSerde(): Serde<DialogmeldingStatusDTO> {
    return Serdes.serdeFrom(DialogmeldingStatusDTOSerializer(), DialogmeldingStatusDTODeserializer())
}