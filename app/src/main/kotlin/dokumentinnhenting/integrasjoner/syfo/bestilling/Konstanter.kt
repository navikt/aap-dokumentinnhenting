package dokumentinnhenting.integrasjoner.syfo.bestilling

import no.nav.aap.komponenter.miljo.Miljø

val PÅMINNELSE_ANTALL_DAGER_TREKKE_FRA = if (Miljø.erProd()) 22L else 1L
const val MINSTE_ANTALL_DØGN_SIDEN_FORRIGE_PÅMINNELSE = 1L