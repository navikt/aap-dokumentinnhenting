package dokumentinnhenting.integrasjoner.syfo.bestilling

import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.DialogmeldingType
import dokumentinnhenting.repositories.MottattDialogmeldingRecord
import java.time.LocalDateTime
import java.util.UUID

fun mottatteDialogmeldinger(): List<MottattDialogmeldingRecord> {
    return listOf(
        MottattDialogmeldingRecord(
            id = 3,
            msgId = UUID.randomUUID(),
            msgType = "Type",
            mottattTidspunkt = LocalDateTime.now().minusDays(5),
            conversationRef = null,
            parentRef = null,
            personIdentPasient = "98765",
            legehpr = "999",
            navnHelsepersonell = "Din lege",
            dialogmeldingType = DialogmeldingType.FORESPORSEL_SVAR,
            tekstNotatInnhold = "Here it is",
            dn = "dn",
            journalpostId = "234",
            saksnummer = "LOCAL_1",
            opprettetTid = LocalDateTime.now().minusDays(5),
        )
    )
}

fun sendteDialogmeldinger(): List<DialogmeldingFullRecord> {
    return listOf(
        DialogmeldingFullRecord(
            bestillerNavIdent = "Foo",
            dialogmeldingUuid = UUID.randomUUID(),
            behandlerRef = "BehandlerRef",
            behandlerNavn = "Din behandler",
            behandlerHprNr = "789",
            personIdent = "98765",
            dokumentasjonType = DokumentasjonType.MELDING_FRA_NAV,
            fritekst = "Heisann",
            saksnummer = "7",
            status = MeldingStatusType.SENDT,
            flytStatus = null,
            personNavn = "Personen",
            statusTekst = "Min status",
            behandlingsReferanse = UUID.randomUUID(),
            opprettet = LocalDateTime.now().minusDays(10),
            tidligereBestillingReferanse = null,
            samtaleRef = UUID.randomUUID(),
            journalpostId = "123",
            dokumentId = null,
            id = 1,
            `automatiskPåminnelse` = false
        ),
        DialogmeldingFullRecord(
            bestillerNavIdent = "Foo",
            dialogmeldingUuid = UUID.randomUUID(),
            behandlerRef = "BehandlerRef",
            behandlerNavn = "Din behandler",
            behandlerHprNr = "789",
            personIdent = "98765",
            dokumentasjonType = DokumentasjonType.L40,
            fritekst = "Trenger den legeerklæringa",
            saksnummer = "7",
            status = MeldingStatusType.SENDT,
            flytStatus = null,
            personNavn = "Personen",
            statusTekst = "Min status",
            behandlingsReferanse = UUID.randomUUID(),
            opprettet = LocalDateTime.now().minusDays(3),
            tidligereBestillingReferanse = null,
            samtaleRef = UUID.randomUUID(),
            journalpostId = "123",
            dokumentId = null,
            id = 2,
            `automatiskPåminnelse` = true
        ),
        DialogmeldingFullRecord(
            dialogmeldingUuid = UUID.randomUUID(),
            behandlerNavn = "Foo",
            opprettet = LocalDateTime.now().minusDays(10),
            dokumentasjonType = DokumentasjonType.L40,
            fritekst = "Min tekst",
            status = null,
            journalpostId = "1234",
            bestillerNavIdent = "Nav ident",
            behandlerRef = "Ref",
            behandlerHprNr = "987",
            personIdent = "123",
            saksnummer = "678",
            flytStatus = null,
            personNavn = "Test testesen",
            statusTekst = "Min status",
            behandlingsReferanse = UUID.randomUUID(),
            tidligereBestillingReferanse = null,
            samtaleRef = UUID.randomUUID(),
            dokumentId = "234",
            id = 1,
            `automatiskPåminnelse` = false,
        )
    )
}