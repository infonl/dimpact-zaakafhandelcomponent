/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.zac.util.time.PeriodUtil
import nl.info.client.klant.KlantClientService
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
import nl.info.client.zgw.zrc.util.isWachtOpAanvullendeInformatie
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.model.RestZaakKenmerk
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.toRestGeometry
import nl.info.zac.app.zaak.model.toRestZaakBpmnProcessDefinition
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
    private val restBesluitConverter: RestBesluitConverter,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val bpmnService: BpmnService,
    private val identificationService: IdentificationService,
    private val klantClientService: KlantClientService
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
            .map { restBesluitConverter.convertToRestBesluit(it) }
        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            ?.betrokkeneIdentificatie
            ?.let { restUserConverter.convertUserId(it.identificatie) }
        val initiator = zgwApiService.findInitiatorRoleForZaak(zaak)
        val initiatorIdentificatie = initiator?.let {
            identificationService.createBetrokkeneIdentificatieForInitiatorRole(it)
        }
        val zaakSpecificContactDetails = klantClientService.findZaakSpecificContactDetails(zaak.uuid)
        val zaakData = zaakVariabelenService.readZaakdata(zaak.uuid)
        val hasSentConfirmationOfReceipt = zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid) ?: false
        val bpmnProcessDefinition = bpmnService.findProcessDefinitionByZaak(zaak.uuid)
        return RestZaak(
            archiefActiedatum = zaak.archiefactiedatum,
            archiefNominatie = zaak.archiefnominatie?.name,
            behandelaar = behandelaar,
            besluiten = besluiten,
            bpmnProcessDefinition = bpmnProcessDefinition?.toRestZaakBpmnProcessDefinition(),
            bronorganisatie = zaak.bronorganisatie,
            communicatiekanaal = zaak.communicatiekanaalNaam,
            // 'duur' has the ISO-8601 period format ('P(n)Y(n)M(n)D') in the ZGW ZRC API,
            // so we use [Period.parse] to convert the duration string to a [Period] object
            duurVerlenging = if (zaak.isVerlengd()) PeriodUtil.format(Period.parse(zaak.verlenging.duur)) else null,
            eerdereOpschorting = zaak.opschorting?.eerdereOpschorting ?: false,
            einddatum = zaak.einddatum,
            einddatumGepland = zaak.einddatumGepland,
            gerelateerdeZaken = toRestGerelateerdeZaken(zaakRechten, zaak, loggedInUser),
            groep = groep,
            heeftOntvangstbevestigingVerstuurd = hasSentConfirmationOfReceipt,
            identificatie = zaak.identificatie,
            indicaties = noneOf(ZaakIndicatie::class.java).apply {
                if (zaak.isHoofdzaak()) add(HOOFDZAAK)
                if (zaak.isDeelzaak()) add(DEELZAAK)
                if (statustype.isHeropend()) add(HEROPEND)
                if (zaak.isOpgeschort()) add(OPSCHORTING)
                if (zaak.isVerlengd()) add(VERLENGD)
                if (!hasSentConfirmationOfReceipt && bpmnProcessDefinition == null) {
                    add(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                }
            },
            initiatorIdentificatie = initiatorIdentificatie,
            isBesluittypeAanwezig = zaakType.besluittypen?.isNotEmpty() ?: false,
            isDeelzaak = zaak.isDeelzaak(),
            isHeropend = statustype.isHeropend(),
            isHoofdzaak = zaak.isHoofdzaak(),
            isInIntakeFase = statustype.isIntake() || statustype.isWachtOpAanvullendeInformatie(),
            isOpen = zaak.isOpen(),
            isOpgeschort = zaak.isOpgeschort(),
            isProcesGestuurd = bpmnProcessDefinition != null,
            isVerlengd = zaak.isVerlengd(),
            kenmerken = zaak.kenmerken?.map { RestZaakKenmerk(it.kenmerk, it.bron) },
            omschrijving = zaak.omschrijving,
            publicatiedatum = zaak.publicatiedatum,
            rechten = zaakRechten.toRestZaakRechten(),
            redenOpschorting = takeIf { zaak.isOpgeschort() }?.let { zaak.opschorting?.reden },
            redenVerlenging = if (zaak.isVerlengd()) zaak.verlenging.reden else null,
            registratiedatum = zaak.registratiedatum,
            resultaat = zaak.resultaat?.let(restZaakResultaatConverter::convert),
            startdatum = zaak.startdatum,
            startdatumBewaartermijn = zaak.startdatumBewaartermijn,
            status = status?.takeIf { statustype != null }?.let { toRestZaakStatus(statustype!!, it) },
            toelichting = zaak.toelichting,
            uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening,
            uuid = zaak.uuid,
            verantwoordelijkeOrganisatie = zaak.verantwoordelijkeOrganisatie,
            // use the name because the frontend expects this value to be in uppercase
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name,
            zaakdata = zaakData,
            zaakgeometrie = zaak.zaakgeometrie?.toRestGeometry(),
            zaakSpecificContactDetails = zaakSpecificContactDetails,
            zaaktype = restZaaktypeConverter.convert(zaakType)
        )
    }

    private fun toRestGerelateerdeZaken(
        fromZaakRechten: ZaakRechten,
        zaak: Zaak,
        loggedInUser: LoggedInUser
    ): List<RestGerelateerdeZaak> {
        val gerelateerdeZaken = mutableListOf<RestGerelateerdeZaak>()
        zaak.hoofdzaak?.let {
            gerelateerdeZaken.add(
                restGerelateerdeZaakConverter.convert(
                    fromZaak = zaak,
                    fromZaakRechten = fromZaakRechten,
                    gerelateerdeZaak = zrcClientService.readZaak(it),
                    relatieType = RelatieType.HOOFDZAAK,
                    loggedInUser = loggedInUser
                )
            )
        }
        zaak.deelzaken
            ?.map(zrcClientService::readZaak)
            ?.map {
                restGerelateerdeZaakConverter.convert(zaak, fromZaakRechten, it, loggedInUser, RelatieType.DEELZAAK)
            }
            ?.forEach(gerelateerdeZaken::add)
        zaak.gerelateerdeZaken
            ?.map { restGerelateerdeZaakConverter.convert(zaak, fromZaakRechten, it, loggedInUser) }
            ?.forEach(gerelateerdeZaken::add)
        return gerelateerdeZaken
    }
}
