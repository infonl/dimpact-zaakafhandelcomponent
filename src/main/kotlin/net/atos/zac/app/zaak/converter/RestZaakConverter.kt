/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.Verlenging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zaak.model.RESTZaakKenmerk
import net.atos.zac.app.zaak.model.RESTZaakVerlengGegevens
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.app.zaak.model.RestGerelateerdeZaak
import net.atos.zac.app.zaak.model.RestZaak
import net.atos.zac.app.zaak.model.toRestZaakStatus
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.bpmn.BPMNService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.PeriodUtil
import net.atos.zac.zoeken.model.ZaakIndicatie
import java.time.LocalDate
import java.time.Period
import java.util.EnumSet
import java.util.UUID
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

@Suppress("LongParameterList")
class RestZaakConverter @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val brcClientService: BrcClientService,
    private val zgwApiService: ZGWApiService,
    private val restZaakResultaatConverter: RestZaakResultaatConverter,
    private val restGroupConverter: RestGroupConverter,
    private val restGerelateerdeZaakConverter: RestGerelateerdeZaakConverter,
    private val restUserConverter: RestUserConverter,
    private val restBesluitConverter: RestBesluitConverter,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val restRechtenConverter: RestRechtenConverter,
    private val restGeometryConverter: RestGeometryConverter,
    private val policyService: PolicyService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val bpmnService: BPMNService,
) {
    companion object {
        private val LOG = Logger.getLogger(RestZaakConverter::class.java.name)
    }

    fun toRestZaak(zaak: Zaak): RestZaak {
        val status = zaak.status?.let { zrcClientService.readStatus(it) }
        val statustype = status?.let { ztcClientService.readStatustype(it.statustype) }
        return toRestZaak(zaak, status, statustype)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun toRestZaak(zaak: Zaak, status: Status?, statustype: StatusType?): RestZaak {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val groep = zgwApiService.findGroepForZaak(zaak)
            .map { restGroupConverter.convertGroupId(it.betrokkeneIdentificatie.identificatie) }
            .orElse(null)
        val besluiten = brcClientService.listBesluiten(zaak)
            .map { restBesluitConverter.convertToRestBesluit(it) }
        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            .map { restUserConverter.convertUserId(it.betrokkeneIdentificatie.identificatie) }
            .orElse(null)
        val initiator = zgwApiService.findInitiatorRoleForZaak(zaak)
        return RestZaak(
            identificatie = zaak.identificatie,
            uuid = zaak.uuid,
            besluiten = besluiten,
            bronorganisatie = zaak.bronorganisatie,
            verantwoordelijkeOrganisatie = zaak.verantwoordelijkeOrganisatie,
            startdatum = zaak.startdatum,
            einddatum = zaak.einddatum,
            einddatumGepland = zaak.einddatumGepland,
            uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening,
            publicatiedatum = zaak.publicatiedatum,
            registratiedatum = zaak.registratiedatum,
            archiefNominatie = zaak.archiefnominatie?.name,
            archiefActiedatum = zaak.archiefactiedatum,
            omschrijving = zaak.omschrijving,
            toelichting = zaak.toelichting,
            zaaktype = restZaaktypeConverter.convert(zaaktype),
            status = status?.let { toRestZaakStatus(it, statustype!!) },
            resultaat = zaak.resultaat?.let(restZaakResultaatConverter::convert),
            isOpgeschort = zaak.isOpgeschort,
            redenOpschorting = takeIf { zaak.isOpgeschort }?.let { zaak.opschorting?.reden },
            isVerlengd = zaak.isVerlengd,
            duurVerlenging = if (zaak.isVerlengd) PeriodUtil.format(zaak.verlenging.duur) else null,
            redenVerlenging = if (zaak.isVerlengd) zaak.verlenging.reden else null,
            gerelateerdeZaken = toRestGerelateerdeZaken(zaak),
            zaakgeometrie = zaak.zaakgeometrie?.let { restGeometryConverter.convert(zaak.zaakgeometrie) },
            kenmerken = zaak.kenmerken?.map { RESTZaakKenmerk(it.kenmerk, it.bron) },
            communicatiekanaal = zaak.communicatiekanaalNaam,
            // use the name because the frontend expects this value to be in uppercase
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name,
            groep = groep,
            behandelaar = behandelaar,
            initiatorIdentificatie = initiator.getOrNull()?.identificatienummer,
            initiatorIdentificatieType = when (val betrokkeneType = initiator.getOrNull()?.betrokkeneType) {
                BetrokkeneType.NATUURLIJK_PERSOON -> IdentificatieType.BSN
                BetrokkeneType.VESTIGING -> IdentificatieType.VN
                BetrokkeneType.NIET_NATUURLIJK_PERSOON -> IdentificatieType.RSIN
                // betrokkeneType may be null
                null -> null
                else -> {
                    LOG.warning(
                        "Initiator identificatie type: '$betrokkeneType' is not supported for zaak with UUID: '${zaak.uuid}'"
                    )
                    null
                }
            },
            isHoofdzaak = zaak.is_Hoofdzaak,
            isDeelzaak = zaak.isDeelzaak,
            isOpen = zaak.isOpen,
            isHeropend = StatusTypeUtil.isHeropend(statustype),
            isInIntakeFase = StatusTypeUtil.isIntake(statustype),
            isOntvangstbevestigingVerstuurd = zaakVariabelenService.findOntvangstbevestigingVerstuurd(
                zaak.uuid
            ).orElse(false),
            isBesluittypeAanwezig = zaaktype.besluittypen?.isNotEmpty() ?: false,
            isProcesGestuurd = bpmnService.isProcesGestuurd(zaak.uuid),
            rechten = policyService.readZaakRechten(zaak, zaaktype).let(restRechtenConverter::convert),
            zaakdata = zaakVariabelenService.readZaakdata(zaak.uuid),
            indicaties = when {
                zaak.is_Hoofdzaak -> EnumSet.of(ZaakIndicatie.HOOFDZAAK)
                zaak.isDeelzaak -> EnumSet.of(ZaakIndicatie.DEELZAAK)
                StatusTypeUtil.isHeropend(statustype) -> EnumSet.of(ZaakIndicatie.HEROPEND)
                zaak.isOpgeschort -> EnumSet.of(ZaakIndicatie.OPSCHORTING)
                zaak.isVerlengd -> EnumSet.of(ZaakIndicatie.VERLENGD)
                else -> EnumSet.noneOf(ZaakIndicatie::class.java)
            }
        )
    }

    fun toZaak(restZaak: RestZaak, zaaktype: ZaakType) = Zaak(
        zaaktype.url,
        restZaak.startdatum,
        ConfiguratieService.BRON_ORGANISATIE,
        ConfiguratieService.VERANTWOORDELIJKE_ORGANISATIE
    ).apply {
        this.communicatiekanaalNaam = restZaak.communicatiekanaal
        this.omschrijving = restZaak.omschrijving
        this.toelichting = restZaak.toelichting
        this.registratiedatum = LocalDate.now()
        this.vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            VertrouwelijkheidaanduidingEnum.fromValue(it)
        }
        this.zaakgeometrie = restZaak.zaakgeometrie?.let { restGeometryConverter.convert(it) }
    }

    fun convertToPatch(restZaak: RestZaak): Zaak {
        val zaak = Zaak()
        zaak.toelichting = restZaak.toelichting
        zaak.omschrijving = restZaak.omschrijving
        zaak.startdatum = restZaak.startdatum
        zaak.einddatumGepland = restZaak.einddatumGepland
        zaak.uiterlijkeEinddatumAfdoening = restZaak.uiterlijkeEinddatumAfdoening
        zaak.vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            VertrouwelijkheidaanduidingEnum.fromValue(it)
        }
        zaak.communicatiekanaalNaam = restZaak.communicatiekanaal
        zaak.zaakgeometrie = restZaak.zaakgeometrie?.let { restGeometryConverter.convert(it) }
        return zaak
    }

    fun convertToPatch(zaakUUID: UUID?, verlengGegevens: RESTZaakVerlengGegevens): Zaak {
        val zaak = Zaak()
        zaak.einddatumGepland = verlengGegevens.einddatumGepland
        zaak.uiterlijkeEinddatumAfdoening = verlengGegevens.uiterlijkeEinddatumAfdoening
        val verlenging = zrcClientService.readZaak(zaakUUID).verlenging
        zaak.verlenging = if (verlenging != null && verlenging.duur != null) {
            Verlenging(
                verlengGegevens.redenVerlenging,
                verlenging.duur.plusDays(verlengGegevens.duurDagen.toLong())
            )
        } else {
            Verlenging(
                verlengGegevens.redenVerlenging,
                Period.ofDays(verlengGegevens.duurDagen)
            )
        }
        return zaak
    }

    private fun toRestGerelateerdeZaken(zaak: Zaak): List<RestGerelateerdeZaak> {
        val gerelateerdeZaken = mutableListOf<RestGerelateerdeZaak>()
        zaak.hoofdzaak?.let {
            gerelateerdeZaken.add(
                restGerelateerdeZaakConverter.convert(
                    zrcClientService.readZaak(it),
                    RelatieType.HOOFDZAAK
                )
            )
        }
        zaak.deelzaken?.map { zrcClientService.readZaak(it) }
            ?.map { restGerelateerdeZaakConverter.convert(it, RelatieType.DEELZAAK) }
            ?.forEach { gerelateerdeZaken.add(it) }
        zaak.relevanteAndereZaken?.map { restGerelateerdeZaakConverter.convert(it) }
            ?.forEach { gerelateerdeZaken.add(it) }
        return gerelateerdeZaken
    }
}
