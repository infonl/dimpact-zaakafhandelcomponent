/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.createBesluit
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.zaak.model.createRestBesluit
import net.atos.zac.app.zaak.model.createRestGroup
import net.atos.zac.app.zaak.model.createRestUser
import net.atos.zac.app.zaak.model.createRestZaaktype
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.bpmn.BPMNService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createZaakRechten
import java.util.Optional

class RestZaakConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val brcClientService = mockk<BrcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val restZaakResultaatConverter = mockk<RestZaakResultaatConverter>()
    val restGroupConverter = mockk<RestGroupConverter>()
    val restGerelateerdeZaakConverter = mockk<RestGerelateerdeZaakConverter>()
    val restUserConverter = mockk<RestUserConverter>()
    val restBesluitConverter = mockk<RestBesluitConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
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
        policyService = policyService,
        zaakVariabelenService = zaakVariabelenService,
        bpmnService = bpmnService
    )

    Given("A zaak") {
        val zaak = createZaak()
        val status = createZaakStatus()
        val statusType = createStatusType()
        val zaakType = createZaakType()
        val rolOrganistorischeEenheid = Optional.of(createRolOrganisatorischeEenheid())
        val restGroup = createRestGroup()
        val besluit = createBesluit()
        val restBesluit = createRestBesluit()
        val rolMedewerker = createRolMedewerker()
        val restUser = createRestUser()
        val rol = createRolNatuurlijkPersoon()
        val restZaakType = createRestZaaktype()
        val zaakrechten = createZaakRechten()
        val zaakdata = mapOf("dummyKey" to "dummyValue")

        every { zrcClientService.readStatus(zaak.status) } returns status
        with(ztcClientService) {
            every { readStatustype(status.statustype) } returns statusType
            every { readZaaktype(zaak.zaaktype) } returns zaakType
        }
        with(zgwApiService) {
            every { findGroepForZaak(zaak) } returns rolOrganistorischeEenheid
            every { findBehandelaarMedewerkerRoleForZaak(zaak) } returns Optional.of(rolMedewerker)
            every { findInitiatorRoleForZaak(zaak) } returns Optional.of(rol)
        }
        with(zaakVariabelenService) {
            every { findOntvangstbevestigingVerstuurd(zaak.uuid) } returns Optional.of(false)
            every { readZaakdata(zaak.uuid) } returns zaakdata
        }
        every { restGroupConverter.convertGroupId(rolOrganistorischeEenheid.get().identificatienummer) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
        every { restBesluitConverter.convertToRestBesluit(besluit) } returns restBesluit
        every { restUserConverter.convertUserId(rolMedewerker.identificatienummer) } returns restUser
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.isProcesGestuurd(zaak.uuid) } returns false
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakrechten

        When("converting a zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak)

            Then("the zaak should be converted correctly") {
                with(restZaak) {
                    uuid shouldBe zaak.uuid
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    this.zaaktype shouldBe zaaktype
                }
            }
        }
    }
})
