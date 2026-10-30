package dokumentinnhenting.integrasjoner.syfo.dialogmeldingmottak

import dokumentinnhenting.util.kafka.createGenericSerde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DialogmeldingMottakDTOTest {

    @Test
    fun serde() {
        val json = """
            {
                "msgId": "melding-1",
                "msgType": "DIALOGMELDING",
                "navLogId": "logg-1",
                "mottattTidspunkt": "2026-10-29T12:00:00",
                "conversationRef": "samtale-1",
                "parentRef": "forelder-1",
                "personIdentPasient": "12345678910",
                "pasientAktoerId": "100001",
                "personIdentBehandler": "10987654321",
                "behandlerAktoerId": "100002",
                "legekontorOrgNr": "999999999",
                "legekontorHerId": "12345",
                "legekontorReshId": "67890",
                "legekontorOrgName": "Testlegekontor",
                "legehpr": "7654321",
                "dialogmelding": {
                    "id": "dialogmelding-1",
                    "innkallingMoterespons": {
                        "temaKode": {
                            "kodeverkOID": "2.16.578.1.12.4.1.1.8125",
                            "dn": "Svar",
                            "v": "1",
                            "arenaNotatKategori": "DIALOG",
                            "arenaNotatKode": "SVAR",
                            "arenaNotatTittel": "Svar fra behandler"
                        },
                        "tekstNotatInnhold": "Svar paa innkalling",
                        "dokIdNotat": "notat-1",
                        "foresporsel": {
                            "typeForesp": {
                                "dn": "Foresporsel om pasient",
                                "s": "2.16.578.1.12.4.1.1.8125",
                                "v": "1"
                            },
                            "sporsmal": "Kan du delta?",
                            "dokIdForesp": "foresporsel-1",
                            "rollerRelatertNotat": {
                                "rolleNotat": { "s": "rolle-kodeverk", "v": "1" },
                                "person": { "givenName": "Test", "familyName": "Pasient" },
                                "helsepersonell": { "givenName": "Test", "familyName": "Behandler" }
                            }
                        }
                    },
                    "foresporselFraSaksbehandlerForesporselSvar": {
                        "temaKode": {
                            "kodeverkOID": "2.16.578.1.12.4.1.1.8125",
                            "dn": "Svar",
                            "v": "1",
                            "arenaNotatKategori": "DIALOG",
                            "arenaNotatKode": "SVAR",
                            "arenaNotatTittel": "Svar fra behandler"
                        },
                        "tekstNotatInnhold": "Svar til saksbehandler",
                        "dokIdNotat": "notat-2",
                        "datoNotat": "2026-10-28T10:00:00"
                    },
                    "henvendelseFraLegeHenvendelse": {
                        "temaKode": {
                            "kodeverkOID": "2.16.578.1.12.4.1.1.8125",
                            "dn": "Svar",
                            "v": "1",
                            "arenaNotatKategori": "DIALOG",
                            "arenaNotatKode": "SVAR",
                            "arenaNotatTittel": "Svar fra behandler"
                        },
                        "tekstNotatInnhold": "Henvendelse fra lege",
                        "dokIdNotat": "notat-3",
                        "foresporsel": {
                            "typeForesp": {
                                "dn": "Foresporsel om pasient",
                                "s": "2.16.578.1.12.4.1.1.8125",
                                "v": "1"
                            },
                            "sporsmal": "Hva er status?",
                            "dokIdForesp": "foresporsel-2",
                            "rollerRelatertNotat": {
                                "rolleNotat": { "s": "rolle-kodeverk", "v": "1" },
                                "person": { "givenName": "Test", "familyName": "Pasient" },
                                "helsepersonell": { "givenName": "Test", "familyName": "Behandler" }
                            }
                        },
                        "rollerRelatertNotat": {
                            "rolleNotat": { "s": "rolle-kodeverk", "v": "1" },
                            "person": { "givenName": "Test", "familyName": "Pasient" },
                            "helsepersonell": { "givenName": "Test", "familyName": "Behandler" }
                        }
                    },
                    "navnHelsepersonell": "Test Behandler",
                    "signaturDato": "2026-10-28T11:00:00"
                },
                "antallVedlegg": 1,
                "journalpostId": "journalpost-1",
                "fellesformatXML": "<Dialogmelding>test</Dialogmelding>"
            }
        """.trimIndent()

        val temaKode = TemaKode(
            kodeverkOID = "2.16.578.1.12.4.1.1.8125",
            dn = "Svar",
            v = "1",
            arenaNotatKategori = "DIALOG",
            arenaNotatKode = "SVAR",
            arenaNotatTittel = "Svar fra behandler"
        )
        val rollerRelatertNotat = RollerRelatertNotat(
            rolleNotat = RolleNotat(s = "rolle-kodeverk", v = "1"),
            person = Person(givenName = "Test", familyName = "Pasient"),
            helsepersonell = Helsepersonell(givenName = "Test", familyName = "Behandler")
        )
        val typeForesp = TypeForesp(
            dn = "Foresporsel om pasient",
            s = "2.16.578.1.12.4.1.1.8125",
            v = "1"
        )
        val forventet = DialogmeldingMottakDTO(
            msgId = "melding-1",
            msgType = "DIALOGMELDING",
            mottattTidspunkt = LocalDateTime.of(2026, 10, 29, 12, 0),
            conversationRef = "samtale-1",
            parentRef = "forelder-1",
            personIdentPasient = "12345678910",
            legehpr = "7654321",
            dialogmelding = Dialogmelding(
                id = "dialogmelding-1",
                innkallingMoterespons = InnkallingMoterespons(
                    temaKode = temaKode,
                    tekstNotatInnhold = "Svar paa innkalling",
                    dokIdNotat = "notat-1",
                    foresporsel = Foresporsel(
                        typeForesp = typeForesp,
                        sporsmal = "Kan du delta?",
                        dokIdForesp = "foresporsel-1",
                        rollerRelatertNotat = rollerRelatertNotat
                    )
                ),
                foresporselFraSaksbehandlerForesporselSvar = ForesporselFraSaksbehandlerForesporselSvar(
                    temaKode = temaKode,
                    tekstNotatInnhold = "Svar til saksbehandler",
                    dokIdNotat = "notat-2",
                    datoNotat = LocalDateTime.of(2026, 10, 28, 10, 0)
                ),
                henvendelseFraLegeHenvendelse = HenvendelseFraLegeHenvendelse(
                    temaKode = temaKode,
                    tekstNotatInnhold = "Henvendelse fra lege",
                    dokIdNotat = "notat-3",
                    foresporsel = Foresporsel(
                        typeForesp = typeForesp,
                        sporsmal = "Hva er status?",
                        dokIdForesp = "foresporsel-2",
                        rollerRelatertNotat = rollerRelatertNotat
                    ),
                    rollerRelatertNotat = rollerRelatertNotat
                ),
                navnHelsepersonell = "Test Behandler",
                signaturDato = LocalDateTime.of(2026, 10, 28, 11, 0)
            ),
            journalpostId = "journalpost-1"
        )

        createGenericSerde(DialogmeldingMottakDTO::class.java).use { serde ->
            val topic = "dialogmelding-mottak"
            val deserialisert = serde.deserializer().deserialize(topic, json.toByteArray(Charsets.UTF_8))

            assertEquals(forventet, deserialisert)

            val serialisert = serde.serializer().serialize(topic, deserialisert)
            assertEquals(forventet, serde.deserializer().deserialize(topic, serialisert))
        }
    }

}