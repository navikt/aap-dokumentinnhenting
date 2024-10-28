package dokumentinnhenting.integrasjoner.syfo.bestilling

data class BehandlingsflytToDialogmeldingDTO(
    val behandlerRef: String,               // Referanse til en bestemt behandler, definert i vårt system
    val personIdent: String,                // Personident til den sykmeldte
    //val dialogmeldingRefParent: String or Null Referanse til en annen dialogmelding, dersom denne dialogmeldingen er en oppfølging av en annen dialogmelding
    //val dialogmeldingRefConversation: String Referanse til en samtale. Brukes til å koble sammen melding inn og ut i samme samtaletråd
    val dialogmeldingType: DialogmeldingType,           // Type dialogmelding: DIALOG_FORESPORSEL, DIALOG_SVAR og DIALOG_NOTAT. Når det sendes en DIALOG_FORESPORSEL, kreves det et DIALOG_SVAR tilbake. DIALOG_NOTAT er en melding som ikke krever svar, feks noe som kun er til informasjon.
    val dialogmeldingKodeverk: DialogmeldingKodeverk,       // Kodeverk for dialogmelding: DIALOGMOTE, FORESPORSEL eller HENVENDELSE. Se DialogmeldingToBehandlerBestilling
    val dialogmeldingKode: Int,              // Kode som definerer hva slags dialogmelding dette er, i henhold til kodeverket. Ofte et tall mellom 1-9. Se Dialogmeldingskoder.
    val dialogmeldingTekst: String,          //or Null Innholdet i dialogmeldingen. Kan være en stringserialisering av et dokument på et strukturert format.
    val dialogmeldingVedlegg: ByteArray?,    //Vedlegg til dialogmeldingen, en PDF på byte-array format.
    val sakId: String
)