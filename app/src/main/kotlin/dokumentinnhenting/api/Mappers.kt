package dokumentinnhenting.api

import dokumentinnhenting.integrasjoner.syfo.bestilling.DialogmeldingFullRecord
import dokumentinnhenting.integrasjoner.syfo.bestilling.DokumentasjonType
import dokumentinnhenting.integrasjoner.syfo.status.MeldingStatusType
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto

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
