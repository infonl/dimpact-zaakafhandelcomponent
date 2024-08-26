/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.createBesluit
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createRelevanteZaak
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.app.zaak.model.createRESTBesluit
import net.atos.zac.app.zaak.model.createRESTGerelateerdeZaak
import net.atos.zac.app.zaak.model.createRESTGroup
import net.atos.zac.app.zaak.model.createRESTUser
import net.atos.zac.app.zaak.model.createRESTZaakRechten
import net.atos.zac.app.zaak.model.createRESTZaaktype
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.bpmn.BPMNService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createZaakRechten
import java.net.URI
import java.util.Optional
import java.util.UUID

class RestZaakConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val brcClientService = mockk<BrcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val restZaakResultaatConverter = mockk<RestZaakResultaatConverter>()
    val restGroupConverter = mockk<RestGroupConverter>()
    val restGerelateerdeZaakConverter = mockk<RESTGerelateerdeZaakConverter>()
    val restUserConverter = mockk<RestUserConverter>()
    val restBesluitConverter = mockk<RestBesluitConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val restRechtenConverter = mockk<RESTRechtenConverter>()
    val restGeometryConverter = mockk<RESTGeometryConverter>()
    val policyService = mockk<PolicyService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val bpmnService = mockk<BPMNService>()
    val restZaakConverter = RestZaakConverter(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        brcClientService = brcClientService,
        zgwApiService = zgwApiService,
        restZaakResultaatConverter = restZaakResultaatConverter,
        restGroupConverter = restGroupConverter,
        restGerelateerdeZaakConverter = restGerelateerdeZaakConverter,
        restUserConverter = restUserConverter,
        restBesluitConverter = restBesluitConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        restRechtenConverter = restRechtenConverter,
        restGeometryConverter = restGeometryConverter,
        policyService = policyService,
        zaakVariabelenService = zaakVariabelenService,
        bpmnService = bpmnService
    )

    Given("A zaak with deelzaken, gerelateerde zaken and lots of other data") {
        val zaakStatusUri = URI("https://example.com/${UUID.randomUUID()}")
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType()
        val zaak = createZaak(
            deelzaken = setOf(
                URI("https://example.com/${UUID.randomUUID()}"),
                URI("https://example.com/${UUID.randomUUID()}")
            ),
            relevanteAndereZaken = mutableListOf(createRelevanteZaak(), createRelevanteZaak()),
            status = zaakStatusUri
        )
        val deelZaken = listOf(createZaak(), createZaak())
        val restGerelateerdeZaken = listOf(createRESTGerelateerdeZaak(), createRESTGerelateerdeZaak())
        val zaakType = createZaakType()
        val restZaakType = createRESTZaaktype()
        val rol = createRolOrganisatorischeEenheid()
        val restGroup = createRESTGroup()
        val besluiten = listOf(createBesluit(identificatie = "besluit1"), createBesluit(identificatie = "besluit2"))
        val restBesluiten = listOf(
            createRESTBesluit(identificatie = "besluit1"),
            createRESTBesluit(identificatie = "besluit2")
        )
        val behandelaarRol =
            createRolMedewerker(rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR))
        val initiatorRol =
            createRolNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val restUserBehandelaar = createRESTUser()
        val zaakRechten = createZaakRechten()
        val restZaakRechten = createRESTZaakRechten()
        val zaakData = mapOf("dummyKey" to "dummyValue")

        every { zrcClientService.readStatus(zaakStatusUri) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { zgwApiService.findGroepForZaak(zaak) } returns Optional.of(rol)
        every { restGroupConverter.convertGroupId(rol.betrokkeneIdentificatie.identificatie) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns besluiten
        besluiten.forEachIndexed { index, besluit ->
            every { restBesluitConverter.convertToRestBesluit(besluit) } returns restBesluiten[index]
        }
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns Optional.of(behandelaarRol)
        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns Optional.of(initiatorRol)
        every {
            restUserConverter.convertUserId(behandelaarRol.betrokkeneIdentificatie.identificatie)
        } returns restUserBehandelaar
        zaak.deelzaken.forEachIndexed { index, zaakUri ->
            every { zrcClientService.readZaak(zaakUri) } returns deelZaken[index]
        }
        deelZaken.forEachIndexed { index, deelZaak ->
            every { restGerelateerdeZaakConverter.convert(deelZaak, RelatieType.DEELZAAK) } returns restGerelateerdeZaken[index]
        }
        zaak.relevanteAndereZaken.forEachIndexed { index, relevanteZaak ->
            every { restGerelateerdeZaakConverter.convert(relevanteZaak) } returns restGerelateerdeZaken[index]
        }
        every { zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid) } returns Optional.of(true)
        every { bpmnService.isProcesGestuurd(zaak.uuid) } returns true
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
        every { restRechtenConverter.convert(zaakRechten) } returns restZaakRechten
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakData

        When("the zaak is converted to a REST zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak)

            Then("the zaak is converted correctly") {
                with(restZaak) {
                    this.zaaktype = restZaakType
                    this.besluiten = restBesluiten
                    this.gerelateerdeZaken = restGerelateerdeZaken
                    this.initiatorIdentificatie = zaak.identificatie
                    this.initiatorIdentificatieType = IdentificatieType.BSN
                    this.groep = restGroup
                    with(this.status!!) {
                        this.naam = zaakStatus.statustoelichting
                        this.toelichting = zaakStatus.statustoelichting
                    }
                }
            }
        }
    }
})
