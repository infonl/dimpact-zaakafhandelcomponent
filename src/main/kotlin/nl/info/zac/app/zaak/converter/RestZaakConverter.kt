/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.NATUURLIJK_PERSOON
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.VESTIGING
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.Status
import nl.info.client.zgw.zrc.model.generated.Verlenging
import nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isEerderOpgeschort
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
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.model.RESTZaakKenmerk
import nl.info.zac.app.zaak.model.RESTZaakVerlengGegevens
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.toGeoJSONGeometry
import nl.info.zac.app.zaak.model.toRestGeometry
import nl.info.zac.app.zaak.model.toRestZaakStatus
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.ZaakIndicatie.DEELZAAK
import nl.info.zac.search.model.ZaakIndicatie.HEROPEND
import nl.info.zac.search.model.ZaakIndicatie.HOOFDZAAK
import nl.info.zac.search.model.ZaakIndicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD
import nl.info.zac.search.model.ZaakIndicatie.OPSCHORTING
import nl.info.zac.search.model.ZaakIndicatie.VERLENGD
import java.time.LocalDate
import java.time.Period
import java.util.EnumSet.noneOf
import java.util.UUID
import java.util.logging.Logger

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
    private val restDecisionConverter: RestDecisionConverter,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val policyService: PolicyService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val bpmnService: BpmnService,
    private val configuratieService: ConfiguratieService
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
            isOpgeschort = zaak.isOpgeschort(),
            isEerderOpgeschort = zaak.isEerderOpgeschort(),
            redenOpschorting = takeIf { zaak.isOpgeschort() }?.let { zaak.opschorting?.reden },
            isVerlengd = zaak.isVerlengd(),
            // 'duur' has the ISO-8601 period format ('P(n)Y(n)M(n)D') in the ZGW ZRC API,
            // so we use [Period.parse] to convert the duration string to a [Period] object
            duurVerlenging = if (zaak.isVerlengd()) PeriodUtil.format(Period.parse(zaak.verlenging.duur)) else null,
            redenVerlenging = if (zaak.isVerlengd()) zaak.verlenging.reden else null,
            gerelateerdeZaken = toRestGerelateerdeZaken(zaak),
            zaakgeometrie = zaak.zaakgeometrie?.toRestGeometry(),
            kenmerken = zaak.kenmerken?.map { RESTZaakKenmerk(it.kenmerk, it.bron) },
            communicatiekanaal = zaak.communicatiekanaalNaam,
            // use the name because the frontend expects this value to be in uppercase
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name,
            groep = groep,
            behandelaar = behandelaar,
            initiatorIdentificatie = initiator?.identificatienummer,
            initiatorIdentificatieType = when (val betrokkeneType = initiator?.betrokkeneType) {
                NATUURLIJK_PERSOON -> IdentificatieType.BSN
                VESTIGING -> IdentificatieType.VN
                // niet_natuurlijk_persoon rol type is used for 'RSIN-type' niet-natuurlijke personen but also for vestigingen
                NIET_NATUURLIJK_PERSOON -> (initiator.betrokkeneIdentificatie as NietNatuurlijkPersoonIdentificatie).let {
                    when {
                        it.annIdentificatie?.isNotBlank() == true -> IdentificatieType.RSIN
                        it.vestigingsNummer?.isNotBlank() == true -> IdentificatieType.VN
                        else -> null
                    }
                }
                // betrokkeneType may be null
                null -> null
                else -> {
                    LOG.warning(
                        "Initiator identificatie type: '$betrokkeneType' is not supported for zaak with UUID: '${zaak.uuid}'"
                    )
                    null
                }
            },
            isHoofdzaak = zaak.isHoofdzaak(),
            isDeelzaak = zaak.isDeelzaak(),
            isOpen = zaak.isOpen(),
            isHeropend = statustype.isHeropend(),
            isInIntakeFase = statustype.isIntake(),
            isBesluittypeAanwezig = zaaktype.besluittypen?.isNotEmpty() ?: false,
            isProcesGestuurd = bpmnService.isProcessDriven(zaak.uuid),
            rechten = policyService.readZaakRechten(zaak, zaaktype).toRestZaakRechten(),
            zaakdata = zaakVariabelenService.readZaakdata(zaak.uuid),
            indicaties = noneOf(ZaakIndicatie::class.java).apply {
                if (zaak.isHoofdzaak()) add(HOOFDZAAK)
                if (zaak.isDeelzaak()) add(DEELZAAK)
                if (statustype.isHeropend()) add(HEROPEND)
                if (zaak.isOpgeschort()) add(OPSCHORTING)
                if (zaak.isVerlengd()) add(VERLENGD)
                if (shouldOntvangstbevestigingNietVerstuurdIndicatieBeSet(zaak, statustype)) {
                    add(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                }
            }
        )
    }

    fun toZaak(restZaak: RestZaak, zaaktype: ZaakType) = Zaak(
        null,
        restZaak.uuid,
        restZaak.einddatum,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    ).apply {
        this.bronorganisatie = configuratieService.readBronOrganisatie()
        this.verantwoordelijkeOrganisatie = configuratieService.readVerantwoordelijkeOrganisatie()
        this.startdatum = restZaak.startdatum
        this.zaaktype = zaaktype.url
        this.communicatiekanaalNaam = restZaak.communicatiekanaal
        this.omschrijving = restZaak.omschrijving
        this.toelichting = restZaak.toelichting
        this.registratiedatum = LocalDate.now()
        this.vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            // convert this enum to uppercase in case the client sends it in lowercase
            VertrouwelijkheidaanduidingEnum.valueOf(it.uppercase())
        }
        this.zaakgeometrie = restZaak.zaakgeometrie?.toGeoJSONGeometry()
    }

    fun convertToPatch(restZaak: RestZaak) = Zaak().apply {
        toelichting = restZaak.toelichting
        omschrijving = restZaak.omschrijving
        startdatum = restZaak.startdatum
        einddatumGepland = restZaak.einddatumGepland
        uiterlijkeEinddatumAfdoening = restZaak.uiterlijkeEinddatumAfdoening
        vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            // convert this enum to uppercase in case the client sends it in lowercase
            VertrouwelijkheidaanduidingEnum.valueOf(it.uppercase())
        }
        communicatiekanaalNaam = restZaak.communicatiekanaal
        zaakgeometrie = restZaak.zaakgeometrie?.toGeoJSONGeometry()
    }

    @Suppress("NestedBlockDepth")
    fun convertToPatch(zaakUUID: UUID, verlengGegevens: RESTZaakVerlengGegevens) =
        zrcClientService.readZaak(zaakUUID).let { zaak ->
            Zaak().apply {
                einddatumGepland = verlengGegevens.einddatumGepland
                uiterlijkeEinddatumAfdoening = verlengGegevens.uiterlijkeEinddatumAfdoening
                verlenging = Verlenging().apply {
                    reden = verlengGegevens.redenVerlenging
                    // 'duur' has the ISO-8601 period format ('P(n)Y(n)M(n)D') in the ZGW ZRC API,
                    // so we use [Period.toString] to convert the duration to that format
                    duur = zaak.verlenging?.duur?.let { Period.ofDays(it.toInt() + verlengGegevens.duurDagen).toString() }
                        ?: Period.ofDays(verlengGegevens.duurDagen).toString()
                }
            }
        }

    private fun toRestGerelateerdeZaken(zaak: Zaak): List<RestGerelateerdeZaak> {
        val gerelateerdeZaken = mutableListOf<RestGerelateerdeZaak>()
        zaak.hoofdzaak?.let {
            gerelateerdeZaken.add(
                restGerelateerdeZaakConverter.convert(
                    zaak = zrcClientService.readZaak(it),
                    relatieType = RelatieType.HOOFDZAAK
                )
            )
        }
        zaak.deelzaken
            ?.map(zrcClientService::readZaak)
            ?.map { restGerelateerdeZaakConverter.convert(it, RelatieType.DEELZAAK) }
            ?.forEach(gerelateerdeZaken::add)
        zaak.relevanteAndereZaken
            ?.map(restGerelateerdeZaakConverter::convert)
            ?.forEach(gerelateerdeZaken::add)
        return gerelateerdeZaken
    }

    private fun shouldOntvangstbevestigingNietVerstuurdIndicatieBeSet(zaak: Zaak, statustype: StatusType?) =
        !zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid).orElse(false) &&
            !statustype.isHeropend()
}
