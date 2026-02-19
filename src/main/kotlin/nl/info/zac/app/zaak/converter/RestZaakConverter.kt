/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Status
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isIntake
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.model.RESTZaakKenmerk
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.toRestGeometry
import nl.info.zac.app.zaak.model.toRestZaakStatus
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identification.IdentificationService
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.ZaakIndicatie.DEELZAAK
import nl.info.zac.search.model.ZaakIndicatie.HEROPEND
import nl.info.zac.search.model.ZaakIndicatie.HOOFDZAAK
import nl.info.zac.search.model.ZaakIndicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD
import nl.info.zac.search.model.ZaakIndicatie.OPSCHORTING
import nl.info.zac.search.model.ZaakIndicatie.VERLENGD
import java.time.Period
import java.util.EnumSet.noneOf

@Suppress("LongParameterList")
class RestZaakConverter @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val brcClientService: BrcClientService,
    private val zgwApiService: ZgwApiService,
    private val restZaakResultaatConverter: RestZaakResultaatConverter,
    private val restGroupConverter: RestGroupConverter,
    private val restGerelateerdeZaakConverter: RestGerelateerdeZaakConverter,
    private val restUserConverter: RestUserConverter,
    private val restDecisionConverter: RestDecisionConverter,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val bpmnService: BpmnService,
    private val identificationService: IdentificationService,
) {
    fun toRestZaak(
        zaak: Zaak,
        zaakType: ZaakType,
        zaakRechten: ZaakRechten,
        loggedInUser: LoggedInUser
    ): RestZaak {
        val status = zaak.status?.let { zrcClientService.readStatus(it) }
        val statustype = status?.let { ztcClientService.readStatustype(it.statustype) }
        return toRestZaak(zaak, zaakType, zaakRechten, loggedInUser, status, statustype)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun toRestZaak(
        zaak: Zaak,
        zaakType: ZaakType,
        zaakRechten: ZaakRechten,
        loggedInUser: LoggedInUser,
        status: Status?,
        statustype: StatusType?
    ): RestZaak {
        val groep = zgwApiService.findGroepForZaak(zaak)?.let { rolOrganisatorischeEenheid ->
            rolOrganisatorischeEenheid.betrokkeneIdentificatie?.let {
                restGroupConverter.convertGroupId(it.identificatie)
            }
        }
        val besluiten = brcClientService.listBesluiten(zaak)
            .map { restDecisionConverter.convertToRestDecision(it) }
        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            ?.betrokkeneIdentificatie
            ?.let { restUserConverter.convertUserId(it.identificatie) }
        val initiator = zgwApiService.findInitiatorRoleForZaak(zaak)

        val hasSentConfirmationOfReceipt = zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid) ?: false
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
            startdatumBewaartermijn = zaak.startdatumBewaartermijn,
            omschrijving = zaak.omschrijving,
            toelichting = zaak.toelichting,
            zaaktype = restZaaktypeConverter.convert(zaakType),
            status = status?.takeIf { statustype != null }?.let { toRestZaakStatus(statustype!!, it) },
            resultaat = zaak.resultaat?.let(restZaakResultaatConverter::convert),
            isOpgeschort = zaak.isOpgeschort(),
            redenOpschorting = takeIf { zaak.isOpgeschort() }?.let { zaak.opschorting?.reden },
            eerdereOpschorting = zaak.opschorting?.eerdereOpschorting ?: false,
            isVerlengd = zaak.isVerlengd(),
            // 'duur' has the ISO-8601 period format ('P(n)Y(n)M(n)D') in the ZGW ZRC API,
            // so we use [Period.parse] to convert the duration string to a [Period] object
            duurVerlenging = if (zaak.isVerlengd()) PeriodUtil.format(Period.parse(zaak.verlenging.duur)) else null,
            redenVerlenging = if (zaak.isVerlengd()) zaak.verlenging.reden else null,
            gerelateerdeZaken = toRestGerelateerdeZaken(zaak, loggedInUser),
            zaakgeometrie = zaak.zaakgeometrie?.toRestGeometry(),
            kenmerken = zaak.kenmerken?.map { RESTZaakKenmerk(it.kenmerk, it.bron) },
            communicatiekanaal = zaak.communicatiekanaalNaam,
            // use the name because the frontend expects this value to be in uppercase
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name,
            groep = groep,
            behandelaar = behandelaar,
            initiatorIdentificatie = initiator?.let {
                identificationService.createBetrokkeneIdentificatieForInitiatorRole(
                    it
                )
            },
            isHoofdzaak = zaak.isHoofdzaak(),
            isDeelzaak = zaak.isDeelzaak(),
            isOpen = zaak.isOpen(),
            isHeropend = statustype.isHeropend(),
            isInIntakeFase = statustype.isIntake(),
            isBesluittypeAanwezig = zaakType.besluittypen?.isNotEmpty() ?: false,
            isProcesGestuurd = bpmnService.isZaakProcessDriven(zaak.uuid),
            heeftOntvangstbevestigingVerstuurd = hasSentConfirmationOfReceipt,
            rechten = zaakRechten.toRestZaakRechten(),
            zaakdata = zaakVariabelenService.readZaakdata(zaak.uuid),
            indicaties = noneOf(ZaakIndicatie::class.java).apply {
                if (zaak.isHoofdzaak()) add(HOOFDZAAK)
                if (zaak.isDeelzaak()) add(DEELZAAK)
                if (statustype.isHeropend()) add(HEROPEND)
                if (zaak.isOpgeschort()) add(OPSCHORTING)
                if (zaak.isVerlengd()) add(VERLENGD)
                if (!hasSentConfirmationOfReceipt) {
                    add(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                }
            }
        )
    }

    private fun toRestGerelateerdeZaken(zaak: Zaak, loggedInUser: LoggedInUser): List<RestGerelateerdeZaak> {
        val gerelateerdeZaken = mutableListOf<RestGerelateerdeZaak>()
        zaak.hoofdzaak?.let {
            gerelateerdeZaken.add(
                restGerelateerdeZaakConverter.convert(
                    zaak = zrcClientService.readZaak(it),
                    relatieType = RelatieType.HOOFDZAAK,
                    loggedInUser = loggedInUser
                )
            )
        }
        zaak.deelzaken
            ?.map(zrcClientService::readZaak)
            ?.map { restGerelateerdeZaakConverter.convert(it, loggedInUser, RelatieType.DEELZAAK) }
            ?.forEach(gerelateerdeZaken::add)
        zaak.relevanteAndereZaken
            ?.map { restGerelateerdeZaakConverter.convert(it, loggedInUser) }
            ?.forEach(gerelateerdeZaken::add)
        return gerelateerdeZaken
    }
}
