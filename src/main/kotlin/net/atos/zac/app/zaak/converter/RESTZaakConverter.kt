/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.Verlenging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.identity.converter.RESTGroupConverter
import net.atos.zac.app.identity.converter.RESTUserConverter
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.zaak.model.RESTGerelateerdeZaak
import net.atos.zac.app.zaak.model.RESTZaak
import net.atos.zac.app.zaak.model.RESTZaakKenmerk
import net.atos.zac.app.zaak.model.RESTZaakVerlengGegevens
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.PeriodUtil
import net.atos.zac.zoeken.model.ZaakIndicatie
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.Period
import java.util.EnumSet
import java.util.UUID
import java.util.stream.Collectors
import kotlin.jvm.optionals.getOrNull

class RESTZaakConverter {
    @Inject
    private lateinit var ztcClientService: ZtcClientService

    @Inject
    private lateinit var zrcClientService: ZRCClientService

    @Inject
    private lateinit var brcClientService: BrcClientService

    @Inject
    private lateinit var zgwApiService: ZGWApiService

    @Inject
    private lateinit var zaakResultaatConverter: RESTZaakResultaatConverter

    @Inject
    private lateinit var groupConverter: RESTGroupConverter

    @Inject
    private lateinit var gerelateerdeZaakConverter: RESTGerelateerdeZaakConverter

    @Inject
    private lateinit var userConverter: RESTUserConverter

    @Inject
    private lateinit var besluitConverter: RestBesluitConverter

    @Inject
    private lateinit var zaaktypeConverter: RESTZaaktypeConverter

    @Inject
    private lateinit var rechtenConverter: RESTRechtenConverter

    @Inject
    private lateinit var restGeometryConverter: RESTGeometryConverter

    @Inject
    private lateinit var policyService: PolicyService

    @Inject
    private lateinit var zaakVariabelenService: ZaakVariabelenService

    @Inject
    private lateinit var bpmnService: BPMNService

    fun convert(zaak: Zaak): RESTZaak {
        val status = if (zaak.status != null) zrcClientService.readStatus(zaak.status) else null
        val statustype = if (status != null) ztcClientService.readStatustype(status.statustype) else null
        return convert(zaak, status, statustype)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun convert(zaak: Zaak, status: Status?, statustype: StatusType?): RESTZaak {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val groep = zgwApiService.findGroepForZaak(zaak)
            .map { groep -> groupConverter.convertGroupId(groep.betrokkeneIdentificatie.identificatie) }
            .orElse(null)
        val besluiten = brcClientService.listBesluiten(zaak)
            .map { besluiten -> besluitConverter.convertToRESTBesluit(besluiten) }
            .orElse(null)
        val behandelaar = zgwApiService.findBehandelaarForZaak(zaak)
            .map { behandelaar: RolMedewerker ->
                userConverter.convertUserId(behandelaar.betrokkeneIdentificatie.identificatie)
            }
            .orElse(null)
        val initiator = zgwApiService.findInitiatorForZaak(zaak)
        return RESTZaak(
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
            zaaktype = zaaktypeConverter.convert(zaaktype),
            status = status?.let { convertToRESTZaakStatus(it, statustype!!) },
            resultaat = zaak.resultaat?.let { zaakResultaatConverter.convert(it) },
            isOpgeschort = zaak.isOpgeschort,
            redenOpschorting = if (zaak.isOpgeschort || StringUtils.isNotEmpty(zaak.opschorting.reden)) {
                zaak.opschorting.reden
            } else {
                null
            },
            isVerlengd = zaak.isVerlengd,
            duurVerlenging = if (zaak.isVerlengd) PeriodUtil.format(zaak.verlenging.duur) else null,
            redenVerlenging = if (zaak.isVerlengd) zaak.verlenging.reden else null,
            gerelateerdeZaken = convertGerelateerdeZaken(zaak),
            zaakgeometrie = zaak.zaakgeometrie?.let { restGeometryConverter.convert(zaak.zaakgeometrie) },
            kenmerken = zaak.kenmerken?.stream()?.map {
                RESTZaakKenmerk(it.kenmerk, it.bron)
            }?.collect(Collectors.toList()),
            communicatiekanaal = zaak.communicatiekanaalNaam,
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.toString(),
            groep = groep,
            behandelaar = behandelaar,
            initiatorIdentificatie = initiator.getOrNull()?.identificatienummer,
            initiatorIdentificatieType = when (initiator.getOrNull()?.betrokkeneType) {
                BetrokkeneType.NATUURLIJK_PERSOON -> IdentificatieType.BSN
                BetrokkeneType.VESTIGING -> IdentificatieType.VN
                BetrokkeneType.NIET_NATUURLIJK_PERSOON -> IdentificatieType.RSIN
                else -> null
            },
            isHoofdzaak = zaak.is_Hoofdzaak,
            isDeelzaak = zaak.isDeelzaak,
            isOpen = zaak.isOpen,
            isHeropend = StatusTypeUtil.isHeropend(statustype),
            isInIntakeFase = StatusTypeUtil.isIntake(statustype),
            isOntvangstbevestigingVerstuurd = zaakVariabelenService.findOntvangstbevestigingVerstuurd(
                zaak.uuid
            ).orElse(false),
            isBesluittypeAanwezig = CollectionUtils.isNotEmpty(zaaktype.besluittypen),
            isProcesGestuurd = bpmnService.isProcesGestuurd(zaak.uuid),
            rechten = rechtenConverter.convert(policyService.readZaakRechten(zaak, zaaktype)),
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

    fun convert(restZaak: RESTZaak, zaaktype: ZaakType): Zaak {
        val zaak = Zaak(
            zaaktype.url,
            restZaak.startdatum,
            ConfiguratieService.BRON_ORGANISATIE,
            ConfiguratieService.VERANTWOORDELIJKE_ORGANISATIE
        )
        // aanvullen
        zaak.omschrijving = restZaak.omschrijving
        zaak.toelichting = restZaak.toelichting
        zaak.registratiedatum = LocalDate.now()
        zaak.communicatiekanaalNaam = restZaak.communicatiekanaal
        zaak.vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            VertrouwelijkheidaanduidingEnum.fromValue(it)
        }
        zaak.zaakgeometrie = restZaak.zaakgeometrie?.let { restGeometryConverter.convert(it) }
        return zaak
    }

    fun convertToPatch(restZaak: RESTZaak): Zaak {
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

    private fun convertGerelateerdeZaken(zaak: Zaak): List<RESTGerelateerdeZaak> {
        val gerelateerdeZaken: MutableList<RESTGerelateerdeZaak> = ArrayList()
        zaak.hoofdzaak?.let {
            gerelateerdeZaken.add(
                gerelateerdeZaakConverter.convert(
                    zrcClientService.readZaak(it),
                    RelatieType.HOOFDZAAK
                )
            )
        }
        zaak.deelzaken.stream()
            .map { zrcClientService.readZaak(it) }
            .map { gerelateerdeZaakConverter.convert(it, RelatieType.DEELZAAK) }
            .forEach { gerelateerdeZaken.add(it) }
        zaak.relevanteAndereZaken.stream()
            .map { gerelateerdeZaakConverter.convert(it) }
            .forEach { gerelateerdeZaken.add(it) }
        return gerelateerdeZaken
    }
}
