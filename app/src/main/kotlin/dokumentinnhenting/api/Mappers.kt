package dokumentinnhenting.api

import dokumentinnhenting.integrasjoner.saf.AvsenderMottaker
import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import dokumentinnhenting.repositories.MottattDialogmeldingRecord
import no.nav.aap.dokumentinnhenting.kontrakt.AvsenderMottakerDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående
import no.nav.aap.dokumentinnhenting.kontrakt.MeldingStatusDto
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