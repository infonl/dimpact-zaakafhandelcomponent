/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.Verlenging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.time.PeriodUtil
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.zaak.model.RESTZaakKenmerk
import nl.info.zac.app.zaak.model.RESTZaakVerlengGegevens
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestGerelateerdeZaak
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.toGeometry
import nl.info.zac.app.zaak.model.toRestGeometry
import nl.info.zac.app.zaak.model.toRestZaakStatus
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.search.model.ZaakIndicatie
import java.time.LocalDate
import java.time.Period
import java.util.EnumSet
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
            isOpgeschort = zaak.isOpgeschort,
            isEerderOpgeschort = zaak.isEerderOpgeschort,
            redenOpschorting = takeIf { zaak.isOpgeschort }?.let { zaak.opschorting?.reden },
            isVerlengd = zaak.isVerlengd,
            duurVerlenging = if (zaak.isVerlengd) PeriodUtil.format(zaak.verlenging.duur) else null,
            redenVerlenging = if (zaak.isVerlengd) zaak.verlenging.reden else null,
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
            isProcesGestuurd = bpmnService.isProcessDriven(zaak.uuid),
            rechten = policyService.readZaakRechten(zaak, zaaktype).let(RestRechtenConverter::convert),
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
        configuratieService.readBronOrganisatie(),
        configuratieService.readVerantwoordelijkeOrganisatie()
    ).apply {
        this.communicatiekanaalNaam = restZaak.communicatiekanaal
        this.omschrijving = restZaak.omschrijving
        this.toelichting = restZaak.toelichting
        this.registratiedatum = LocalDate.now()
        this.vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            // convert this enum to uppercase in case the client sends it in lowercase
            VertrouwelijkheidaanduidingEnum.valueOf(it.uppercase())
        }
        this.zaakgeometrie = restZaak.zaakgeometrie?.toGeometry()
    }

    fun convertToPatch(restZaak: RestZaak): Zaak {
        val zaak = Zaak()
        zaak.toelichting = restZaak.toelichting
        zaak.omschrijving = restZaak.omschrijving
        zaak.startdatum = restZaak.startdatum
        zaak.einddatumGepland = restZaak.einddatumGepland
        zaak.uiterlijkeEinddatumAfdoening = restZaak.uiterlijkeEinddatumAfdoening
        zaak.vertrouwelijkheidaanduiding = restZaak.vertrouwelijkheidaanduiding?.let {
            // convert this enum to uppercase in case the client sends it in lowercase
            VertrouwelijkheidaanduidingEnum.valueOf(it.uppercase())
        }
        zaak.communicatiekanaalNaam = restZaak.communicatiekanaal
        zaak.zaakgeometrie = restZaak.zaakgeometrie?.toGeometry()
        return zaak
    }

    fun convertToPatch(zaakUUID: UUID, verlengGegevens: RESTZaakVerlengGegevens) =
        zrcClientService.readZaak(zaakUUID).let { zaak ->
            Zaak().apply {
                einddatumGepland = verlengGegevens.einddatumGepland
                uiterlijkeEinddatumAfdoening = verlengGegevens.uiterlijkeEinddatumAfdoening
                verlenging = Verlenging(
                    verlengGegevens.redenVerlenging,
                    zaak.verlenging?.duur?.plusDays(verlengGegevens.duurDagen.toLong()) ?: Period.ofDays(verlengGegevens.duurDagen)
                )
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
}
