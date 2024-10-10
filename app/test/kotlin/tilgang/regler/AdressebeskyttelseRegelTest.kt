package tilgang.regler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tilgang.Rolle
import tilgang.integrasjoner.pdl.Gradering
import tilgang.integrasjoner.pdl.PersonResultat

class AdressebeskyttelseRegelTest {
    @Test
    fun `Saksbehandler uten fortrolige-roller skal ikke kunne lese fortrolige adresser`() {
        val personListe1 = listOf(
            PersonResultat("1111", listOf(Gradering.UGRADERT), "kode"),
            PersonResultat("1000", listOf(Gradering.FORTROLIG), "kode")
        )
        val personListe2 = listOf(PersonResultat("1234", listOf(Gradering.STRENGT_FORTROLIG), "kode"))
        val personListe3 = listOf(PersonResultat("5678", listOf(Gradering.STRENGT_FORTROLIG_UTLAND), "kode"))

        assertFalse(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(Rolle.VEILEDER, Rolle.SAKSBEHANDLER),
                    personListe1
                )
            )
        )
        assertFalse(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(Rolle.VEILEDER, Rolle.SAKSBEHANDLER),
                    personListe2
                )
            )
        )
        assertFalse(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(Rolle.VEILEDER, Rolle.SAKSBEHANDLER),
                    personListe3
                )
            )
        )
    }

    @Test
    fun `Rolle STRENGT_FORTROLIG_ADRESSE har tilgang til person med fortrolig og strengt fortrolig adresse`() {
        val personer = listOf(
            PersonResultat("1000", listOf(Gradering.FORTROLIG), "kode"),
            PersonResultat("1234", listOf(Gradering.STRENGT_FORTROLIG), "kode"),
            PersonResultat("5678", listOf(Gradering.STRENGT_FORTROLIG_UTLAND), "kode")
        )
        assertTrue(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(
                        Rolle.SAKSBEHANDLER,
                        Rolle.STRENGT_FORTROLIG_ADRESSE
                    ), personer
                )
            )
        )
    }

    @Test
    fun `Rolle STRENGT_FORTROLIG_ADRESSE har tilgang til person med strengt fortrolig adresse utland`() {
        val personer = listOf(
            PersonResultat("5678", listOf(Gradering.STRENGT_FORTROLIG_UTLAND), "kode")
        )
        assertTrue(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(
                        Rolle.SAKSBEHANDLER,
                        Rolle.STRENGT_FORTROLIG_ADRESSE
                    ), personer
                )
            )
        )
    }

    @Test
    fun `Rolle FORTROLIG_ADRESSE har tilgang til person med fortrolig, men ikke strengt fortrolig, adresse`() {
        val personListe1 = listOf(PersonResultat("1000", listOf(Gradering.FORTROLIG), "kode"))
        val personListe2 = listOf(
            PersonResultat("1000", listOf(Gradering.FORTROLIG), "kode"),
            PersonResultat("1234", listOf(Gradering.STRENGT_FORTROLIG), "kode"),
            PersonResultat("5678", listOf(Gradering.STRENGT_FORTROLIG_UTLAND), "kode")
        )
        assertTrue(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(
                        Rolle.VEILEDER,
                        Rolle.FORTROLIG_ADRESSE
                    ), personListe1
                )
            )
        )
        assertFalse(
            AdressebeskyttelseRegel.vurder(
                AdressebeskyttelseInput(
                    listOf(
                        Rolle.VEILEDER,
                        Rolle.FORTROLIG_ADRESSE
                    ), personListe2
                )
            )
        )
    }
}