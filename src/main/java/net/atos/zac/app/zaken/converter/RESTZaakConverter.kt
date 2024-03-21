/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.vrl.VRLClientService
import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.client.zgw.brc.BRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.util.InformatieobjectenUtil
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.Verlenging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.app.identity.converter.RESTGroupConverter
import net.atos.zac.app.identity.converter.RESTUserConverter
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.zaken.model.RESTGerelateerdeZaak
import net.atos.zac.app.zaken.model.RESTZaak
import net.atos.zac.app.zaken.model.RESTZaakKenmerk
import net.atos.zac.app.zaken.model.RESTZaakVerlengGegevens
import net.atos.zac.app.zaken.model.RelatieType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.PeriodUtil
import net.atos.zac.util.UriUtil
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors
import kotlin.jvm.optionals.getOrNull

class RESTZaakConverter {
    @Inject
    private lateinit var ztcClientService: ZTCClientService

    @Inject
    private lateinit var zrcClientService: ZRCClientService

    @Inject
    private lateinit var brcClientService: BRCClientService

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
    private lateinit var besluitConverter: RESTBesluitConverter

    @Inject
    private lateinit var zaaktypeConverter: RESTZaaktypeConverter

    @Inject
    private lateinit var rechtenConverter: RESTRechtenConverter

    @Inject
    private lateinit var vrlClientService: VRLClientService

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

    @Suppress("LongMethod")
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
        val communicatiekanaal = zaak.communicatiekanaal?.let {
            vrlClientService.findCommunicatiekanaal(UriUtil.uuidFromURI(zaak.communicatiekanaal))
                .map { communicatieKanaal -> convertToRESTCommunicatiekanaal(communicatieKanaal) }
                .orElse(null)
        }
        val initiator = zgwApiService.findInitiatorForZaak(zaak)
        val restZaak = RESTZaak(
            identificatie = zaak.identificatie,
            uuid = zaak.uuid,
            besluiten = besluiten,
            eigenschappen = null, // TODO: not used
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
            resultaat = zaakResultaatConverter.convert(zaak.resultaat),
            isOpgeschort = zaak.isOpgeschort,
            redenOpschorting = (zaak.isOpgeschort || StringUtils.isNotEmpty(zaak.opschorting.reden)).let {
                zaak.opschorting.reden
            },
            isVerlengd = zaak.isVerlengd,
            duurVerlenging = zaak.isVerlengd.let { PeriodUtil.format(zaak.verlenging.duur) },
            redenVerlenging = zaak.isVerlengd.let { zaak.verlenging.reden },
            gerelateerdeZaken = convertGerelateerdeZaken(zaak),
            zaakgeometrie = zaak.zaakgeometrie?.let { restGeometryConverter.convert(zaak.zaakgeometrie) },
            kenmerken = zaak.kenmerken?.let {
                zaak.kenmerken.stream()
                    .map { zaakKenmerk -> RESTZaakKenmerk(zaakKenmerk.kenmerk, zaakKenmerk.bron) }
                    .collect(Collectors.toList())
            },
            communicatiekanaal = communicatiekanaal,
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.toString(),
            groep = groep,
            behandelaar = behandelaar,
            initiatorIdentificatie = initiator.getOrNull()?.identificatienummer,
            initiatorIdentificatieType = initiator.isPresent.let {
                when (initiator.get().betrokkeneType) {
                    BetrokkeneType.NATUURLIJK_PERSOON -> IdentificatieType.BSN
                    BetrokkeneType.VESTIGING -> IdentificatieType.VN
                    BetrokkeneType.NIET_NATUURLIJK_PERSOON -> IdentificatieType.RSIN
                    else -> null
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
            isBesluittypeAanwezig = CollectionUtils.isNotEmpty(zaaktype.besluittypen),
            isProcesGestuurd = bpmnService.isProcesGestuurd(zaak.uuid),
            rechten = rechtenConverter.convert(policyService.readZaakRechten(zaak, zaaktype)),
            zaakdata = zaakVariabelenService.readZaakdata(zaak.uuid)
        )

        return restZaak
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
        restZaak.communicatiekanaal?.let {
            vrlClientService.findCommunicatiekanaal(restZaak.communicatiekanaal.uuid)
                .map { obj: CommunicatieKanaal -> obj.url }
                .ifPresent { communicatiekanaal -> zaak.communicatiekanaal = communicatiekanaal }
        }
        zaak.vertrouwelijkheidaanduiding = InformatieobjectenUtil.convertToVertrouwelijkheidaanduidingEnum(
            restZaak.vertrouwelijkheidaanduiding
        )
        zaak.zaakgeometrie = restZaak.zaakgeometrie?.let { restGeometryConverter.convert(restZaak.zaakgeometrie) }
        return zaak
    }

    fun convertToPatch(restZaak: RESTZaak): Zaak {
        val zaak = Zaak()
        zaak.toelichting = restZaak.toelichting
        zaak.omschrijving = restZaak.omschrijving
        zaak.startdatum = restZaak.startdatum
        zaak.einddatumGepland = restZaak.einddatumGepland
        zaak.uiterlijkeEinddatumAfdoening = restZaak.uiterlijkeEinddatumAfdoening
        zaak.vertrouwelijkheidaanduiding = InformatieobjectenUtil.convertToVertrouwelijkheidaanduidingEnum(
            restZaak.vertrouwelijkheidaanduiding
        )
        restZaak.communicatiekanaal?.let {
            vrlClientService.findCommunicatiekanaal(restZaak.communicatiekanaal.uuid)
                .map { obj -> obj.url }
                .ifPresent { communicatiekanaal -> zaak.communicatiekanaal = communicatiekanaal }
        }
        zaak.zaakgeometrie = restZaak.zaakgeometrie?.let { restGeometryConverter.convert(restZaak.zaakgeometrie) }
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
        if (zaak.hoofdzaak != null) {
            gerelateerdeZaken.add(
                gerelateerdeZaakConverter.convert(
                    zrcClientService.readZaak(zaak.hoofdzaak),
                    RelatieType.HOOFDZAAK
                )
            )
        }
        zaak.deelzaken.stream()
            .map { zaakURI -> zrcClientService.readZaak(zaakURI) }
            .map { deelzaak -> gerelateerdeZaakConverter.convert(deelzaak, RelatieType.DEELZAAK) }
            .forEach { restGerelateerdeZaak -> gerelateerdeZaken.add(restGerelateerdeZaak) }
        zaak.relevanteAndereZaken.stream()
            .map { relevanteZaak -> gerelateerdeZaakConverter.convert(relevanteZaak) }
            .forEach { restGerelateerdeZaak -> gerelateerdeZaken.add(restGerelateerdeZaak) }
        return gerelateerdeZaken
    }
}
