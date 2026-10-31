package dokumentinnhenting.api

import dokumentinnhenting.integrasjoner.saf.AvsenderMottaker
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.bestilling.PÅMINNELSE_ANTALL_DAGER_TREKKE_FRA
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.PåminnelseStatus
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.MottattDialogmeldingRecord
import no.nav.aap.dokumentinnhenting.kontrakt.AvsenderMottakerDto
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående
import no.nav.aap.dokumentinnhenting.kontrakt.MeldingStatusDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun DialogmeldingFullRecord.tilDto(): DialogmeldingStatusTilBehandslingsflytDto {
    return DialogmeldingStatusTilBehandslingsflytDto(
        dialogmeldingUuid = this.dialogmeldingUuid,
        status = this.status?.mapStatus(),
        statusTekst = this.statusTekst,
        behandlerRef = this.behandlerRef,
        behandlerNavn = this.behandlerNavn,
        personId = this.personIdent,
        saksnummer = this.saksnummer,
        opprettet = this.opprettet,
        behandlingsReferanse = this.behandlingsReferanse,
        fritekst = this.fritekst
    )
}

fun MeldingStatusType.mapStatus(): DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType {
    return when (this) {
        MeldingStatusType.BESTILT -> DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType.BESTILT
        MeldingStatusType.SENDT -> DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType.SENDT
        MeldingStatusType.OK -> DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType.OK
        MeldingStatusType.AVVIST -> DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType.AVVIST
        MeldingStatusType.MOTTATT -> DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType.MOTTATT
    }
}

fun MeldingStatusType.mapLeveringStatus(): MeldingStatusDto {
    return when (this) {
        MeldingStatusType.BESTILT -> MeldingStatusDto.SENDT
        MeldingStatusType.SENDT -> MeldingStatusDto.SENDT
        MeldingStatusType.OK -> MeldingStatusDto.LEVERT
        MeldingStatusType.AVVIST -> MeldingStatusDto.FEILET
        MeldingStatusType.MOTTATT -> MeldingStatusDto.LEVERT
    }
}

fun AvsenderMottaker.AvsenderMottakerIdType.tilKontrakt(): AvsenderMottakerDto.AvsenderMottakerIdType {
    return when (this) {
        AvsenderMottaker.AvsenderMottakerIdType.FNR -> AvsenderMottakerDto.AvsenderMottakerIdType.FNR
        AvsenderMottaker.AvsenderMottakerIdType.ORGNR -> AvsenderMottakerDto.AvsenderMottakerIdType.ORGNR
        AvsenderMottaker.AvsenderMottakerIdType.HPRNR -> AvsenderMottakerDto.AvsenderMottakerIdType.HPRNR
        AvsenderMottaker.AvsenderMottakerIdType.UTL_ORG -> AvsenderMottakerDto.AvsenderMottakerIdType.UTL_ORG
        AvsenderMottaker.AvsenderMottakerIdType.NULL -> AvsenderMottakerDto.AvsenderMottakerIdType.NULL
        AvsenderMottaker.AvsenderMottakerIdType.UKJENT -> AvsenderMottakerDto.AvsenderMottakerIdType.UKJENT
    }
}

fun no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.fraDto(): DokumentasjonType {
    return when (this) {
        no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L40 -> DokumentasjonType.L40
        no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L8 -> DokumentasjonType.L8
        no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L120 -> DokumentasjonType.L120
        no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.MELDING_FRA_NAV -> DokumentasjonType.MELDING_FRA_NAV
        no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.RETUR_LEGEERKLÆRING -> DokumentasjonType.RETUR_LEGEERKLÆRING
        no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.PURRING -> DokumentasjonType.PURRING
    }
}

fun DokumentasjonType.tilDto(): no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType {
    return when (this) {
        DokumentasjonType.L40 -> no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L40
        DokumentasjonType.L8 -> no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L8
        DokumentasjonType.L120 -> no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.L120
        DokumentasjonType.MELDING_FRA_NAV -> no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.MELDING_FRA_NAV
        DokumentasjonType.RETUR_LEGEERKLÆRING -> no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.RETUR_LEGEERKLÆRING
        DokumentasjonType.PURRING -> no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType.PURRING
    }
}

fun DialogmeldingRecord.tilPåminnelseRecord(
    påminnelseDato: LocalDate,
    opprettetTidspunkt: LocalDateTime
): PåminnelseRecord {
    return PåminnelseRecord(
        id = 1,
        dialogmeldingId = this.dialogmeldingUuid,
        behandlingsReferanse = this.behandlingsReferanse,
        status = PåminnelseStatus.PLANLAGT,
        påminnelseDato = påminnelseDato,
        opprettetTidspunkt = opprettetTidspunkt,
        sisteForespørselSendt = null
    )
}

fun BehandlingsflytToDokumentInnhentingBestillingDto.tilDialogmeldingRecord(
    dialogmeldingUuid: UUID, samtaleRef: UUID? = null
): DialogmeldingRecord {
    return DialogmeldingRecord(
        bestillerNavIdent = this.bestillerNavIdent,
        dialogmeldingUuid = dialogmeldingUuid,
        behandlerRef = this.behandlerRef,
        behandlerHprNr = this.behandlerHprNr,
        personIdent = this.personIdent,
        personNavn = this.personNavn,
        saksnummer = this.saksnummer,
        dokumentasjonType = this.dokumentasjonType.fraDto(),
        behandlerNavn = this.behandlerNavn,
        fritekst = this.dialogmeldingTekst,
        behandlingsReferanse = this.behandlingsReferanse,
        tidligereBestillingReferanse = this.tidligereBestillingReferanse,
        samtaleRef = samtaleRef ?: dialogmeldingUuid,
    )
}

fun DialogmeldingFullRecord.tilFellesDialogmeldingDto(
    kanPåminnesMedSisteForespørselDato: Map<UUID, LocalDateTime?>
): FellesDialogmeldingDto {
    return FellesDialogmeldingDto(
        dialogmeldingReferanse = requireNotNull(this.dialogmeldingUuid) {
            "Utgående dialogmelding må ha en referanse"
        },
        innkommendeUtgående = InnkommendeUtgående.UTGÅENDE,
        meldingFraNavn = this.behandlerNavn,
        opprettetTidspunkt = this.opprettet,
        dokumentasjonsType = this.dokumentasjonType.tilDto(),
        tekst = this.fritekst,
        meldingStatus = this.status?.mapLeveringStatus(),
        journalpostId = this.journalpostId,
        automatiskPåminnelse = if (this.dokumentasjonType == DokumentasjonType.L40) {
            this.automatiskPåminnelse
        } else {
            null
        },
        kanSendeManuellPåminnelse = kanPåminnesMedSisteForespørselDato.contains(this.dialogmeldingUuid),
        sisteForespørselSendt = kanPåminnesMedSisteForespørselDato[this.dialogmeldingUuid]
    )
}

fun DialogmeldingFullRecord.tilPåminnelseRecord(
    antallDagerPåminnelseFrist: Long = PÅMINNELSE_ANTALL_DAGER_TREKKE_FRA,
    påminnelseStatus: PåminnelseStatus? = null,
): PåminnelseRecord {
    require(antallDagerPåminnelseFrist > 0) {
        "Antallet dager frist på påminnelse må være et positivt tall!"
    }

    val påminnelseFrist = this.opprettet.plusDays(antallDagerPåminnelseFrist)
    val erAutomatiskPåminnelseDatoPassert = påminnelseFrist < LocalDateTime.now()
    val utledetStatus =
        // Hvis det har blitt sendt en manuell påminnelse (etter fristen på 3 uker), så skal påminnelsen eksistere i
        // tabellen allerede, og vi kan derfor se bort fra det scenarioet.
        if (erAutomatiskPåminnelseDatoPassert)
            // Antar her at 'automatiskPåminnelse' ikke kan bli endret etter påminnelsesdatoen har passert
            if (this.automatiskPåminnelse)
                // Har den blitt sendt så vil den eksistere med status SENDT allerede
                PåminnelseStatus.PLANLAGT
            else
                PåminnelseStatus.AVBRUTT
        else
            if (this.automatiskPåminnelse)
                PåminnelseStatus.PLANLAGT
            else
                PåminnelseStatus.AVBRUTT

    val statusTilLagring = påminnelseStatus ?: utledetStatus
    val antattSisteForespørselSendt = if (statusTilLagring == PåminnelseStatus.SENDT) påminnelseFrist else null

    return PåminnelseRecord(
        id = -1,        // Blir satt i DB
        dialogmeldingId = this.dialogmeldingUuid,
        behandlingsReferanse = this.behandlingsReferanse,
        status = statusTilLagring,
        påminnelseDato = påminnelseFrist.toLocalDate(),
        opprettetTidspunkt = this.opprettet, // Burde vært opprettet samtidig som forespørselen
        sisteForespørselSendt = antattSisteForespørselSendt
    )
}

fun MottattDialogmeldingRecord.tilFellesDialogmeldingDto(): FellesDialogmeldingDto {
    return FellesDialogmeldingDto(
        dialogmeldingReferanse = null,
        innkommendeUtgående = InnkommendeUtgående.INNKOMMENDE,
        meldingFraNavn = this.navnHelsepersonell,
        opprettetTidspunkt = this.opprettetTid,
        dokumentasjonsType = null,
        tekst = this.tekstNotatInnhold,
        meldingStatus = null,
        journalpostId = this.journalpostId,
        kanSendeManuellPåminnelse = false,
        sisteForespørselSendt = null,
    )
}