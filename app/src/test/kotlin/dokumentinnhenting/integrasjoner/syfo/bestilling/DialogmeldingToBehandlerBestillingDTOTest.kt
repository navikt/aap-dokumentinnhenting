package dokumentinnhenting.integrasjoner.syfo.bestilling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DialogmeldingToBehandlerBestillingDTOTest {

    private val objectMapper = jacksonObjectMapper()

    private fun lagDto(
        dialogmeldingRefParent: String? = null,
        dialogmeldingTekst: String? = "noe tekst",
        dialogmeldingKode: DialogmeldingKode = DialogmeldingKode.FORESPØRSEL_OM_PASIENT
    ) = DialogmeldingToBehandlerBestillingDTO(
        behandlerRef = UUID.randomUUID().toString(),
        personIdent = "12345678910",
        dialogmeldingUuid = UUID.fromString("11111111-2222-3333-4444-555555555555"),
        dialogmeldingRefParent = dialogmeldingRefParent,
        dialogmeldingRefConversation = UUID.randomUUID().toString(),
        dialogmeldingType = DialogmeldingType.DIALOG_FORESPORSEL,
        dialogmeldingKodeverk = DialogmeldingKodeverk.FORESPORSEL,
        dialogmeldingKode = dialogmeldingKode,
        dialogmeldingTekst = dialogmeldingTekst,
        dialogmeldingVedlegg = byteArrayOf(1, 2, 3, 4),
        kilde = "AAP",
    )

    @Test
    fun `serialiserer og deserialiserer korrekt`() {
        val original = lagDto()

        val json = objectMapper.writeValueAsString(original)
        val deserialized = objectMapper.readValue<DialogmeldingToBehandlerBestillingDTO>(json)

        assertEquals(original.behandlerRef, deserialized.behandlerRef)
        assertEquals(original.personIdent, deserialized.personIdent)
        assertEquals(original.dialogmeldingUuid, deserialized.dialogmeldingUuid)
        assertEquals(original.dialogmeldingRefParent, deserialized.dialogmeldingRefParent)
        assertEquals(original.dialogmeldingRefConversation, deserialized.dialogmeldingRefConversation)
        assertEquals(original.dialogmeldingType, deserialized.dialogmeldingType)
        assertEquals(original.dialogmeldingKodeverk, deserialized.dialogmeldingKodeverk)
        assertEquals(original.dialogmeldingKode, deserialized.dialogmeldingKode)
        assertEquals(original.dialogmeldingTekst, deserialized.dialogmeldingTekst)
        assertArrayEquals(original.dialogmeldingVedlegg, deserialized.dialogmeldingVedlegg)
        assertEquals(original.kilde, deserialized.kilde)
    }

    @ParameterizedTest
    @EnumSource(DialogmeldingKode::class)
    fun `dialogmeldingKode serialiserer som heltall`(kode: DialogmeldingKode) {
        val dto = lagDto(dialogmeldingKode = kode)
        val json = objectMapper.writeValueAsString(dto)
        val tree = objectMapper.readTree(json)

        assertEquals(kode.kode, tree["dialogmeldingKode"].intValue())
    }

    @Test
    fun `nullable felt er null i JSON når de ikke er satt`() {
        val dto = lagDto(dialogmeldingRefParent = null, dialogmeldingTekst = null)
        val json = objectMapper.writeValueAsString(dto)
        val tree = objectMapper.readTree(json)

        assert(tree["dialogmeldingRefParent"].isNull)
        assert(tree["dialogmeldingTekst"].isNull)
    }

    @Test
    fun `alle DialogmeldingKode-verdier round-tripper korrekt`() {
        DialogmeldingKode.entries.forEach { kode ->
            val dto = lagDto().copy(dialogmeldingKode = kode)
            val json = objectMapper.writeValueAsString(dto)
            val deserialized = objectMapper.readValue<DialogmeldingToBehandlerBestillingDTO>(json)
            assertEquals(kode, deserialized.dialogmeldingKode)
        }
    }
}
