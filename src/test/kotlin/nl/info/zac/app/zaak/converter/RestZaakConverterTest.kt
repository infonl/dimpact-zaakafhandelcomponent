/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.util.isEerderOpgeschort
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.zaak.model.createRestDecision
import nl.info.zac.app.zaak.model.createRestGroup
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.app.zaak.model.createRestZaaktype
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_HEROPEND
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.ZaakIndicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD
import java.util.EnumSet
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
    val restDecisionConverter = mockk<RestDecisionConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val policyService = mockk<PolicyService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val bpmnService = mockk<BpmnService>()
    val configuratieService = mockk<ConfiguratieService>()
    val restZaakConverter = RestZaakConverter(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        brcClientService = brcClientService,
        zgwApiService = zgwApiService,
        restZaakResultaatConverter = restZaakResultaatConverter,
        restGroupConverter = restGroupConverter,
        restGerelateerdeZaakConverter = restGerelateerdeZaakConverter,
        restUserConverter = restUserConverter,
        restDecisionConverter = restDecisionConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        policyService = policyService,
        zaakVariabelenService = zaakVariabelenService,
        bpmnService = bpmnService,
        configuratieService = configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaak") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val rolOrganistorischeEenheid = createRolOrganisatorischeEenheid()
        val restGroup = createRestGroup()
        val besluit = createBesluit()
        val restBesluit = createRestDecision()
        val rolMedewerker = createRolMedewerker()
        val restUser = createRestUser()
        val rol = createRolNatuurlijkPersoon()
        val restZaakType = createRestZaaktype()
        val zaakrechten = createZaakRechten()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        with(ztcClientService) {
            every { readZaaktype(zaak.zaaktype) } returns zaakType
        }
        with(zgwApiService) {
            every { findGroepForZaak(zaak) } returns rolOrganistorischeEenheid
            every { findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerker
            every { findInitiatorRoleForZaak(zaak) } returns rol
        }
        with(zaakVariabelenService) {
            every { findOntvangstbevestigingVerstuurd(zaak.uuid) } returns Optional.of(false)
            every { readZaakdata(zaak.uuid) } returns zaakdata
        }
        every { restGroupConverter.convertGroupId(rolOrganistorischeEenheid.identificatienummer) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
        every { restDecisionConverter.convertToRestDecision(besluit) } returns restBesluit
        every { restUserConverter.convertUserId(rolMedewerker.identificatienummer) } returns restUser
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.isProcessDriven(zaak.uuid) } returns false
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
                    isVerlengd shouldBe zaak.isVerlengd()
                    isOpgeschort shouldBe zaak.isOpgeschort()
                    isEerderOpgeschort shouldBe zaak.isEerderOpgeschort()
                    indicaties shouldContainExactly EnumSet.of(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                }
            }
        }
    }

    Given("A zaak was closed but reopened") {
        val status = createZaakStatus()
        status.statustoelichting = "fake status"
        val statusType = createStatusType()
            .apply {
                omschrijving = STATUSTYPE_OMSCHRIJVING_HEROPEND
            }
        val zaak = createZaak().apply {
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        }
        val zaakType = createZaakType()
        val rolOrganistorischeEenheid = createRolOrganisatorischeEenheid()
        val restGroup = createRestGroup()
        val besluit = createBesluit()
        val restBesluit = createRestDecision()
        val rolMedewerker = createRolMedewerker()
        val restUser = createRestUser()
        val rol = createRolNatuurlijkPersoon()
        val restZaakType = createRestZaaktype()
        val zaakrechten = createZaakRechten()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        with(ztcClientService) {
            every { readZaaktype(zaak.zaaktype) } returns zaakType
        }
        with(zgwApiService) {
            every { findGroepForZaak(zaak) } returns rolOrganistorischeEenheid
            every { findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerker
            every { findInitiatorRoleForZaak(zaak) } returns rol
        }
        with(zaakVariabelenService) {
            every { readZaakdata(zaak.uuid) } returns zaakdata
        }
        every { restGroupConverter.convertGroupId(rolOrganistorischeEenheid.identificatienummer) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
        every { restDecisionConverter.convertToRestDecision(besluit) } returns restBesluit
        every { restUserConverter.convertUserId(rolMedewerker.identificatienummer) } returns restUser
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.isProcessDriven(zaak.uuid) } returns false
        every { policyService.readZaakRechten(zaak, zaakType) } returns zaakrechten

        When("converting a zaak to a rest zaak") {
            every {
                zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid)
            } returns Optional.of(true)

            val restZaak = restZaakConverter.toRestZaak(zaak, status, statusType)

            Then("the zaak should be converted correctly") {
                with(restZaak) {
                    uuid shouldBe zaak.uuid
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    this.zaaktype shouldBe zaaktype
                    isVerlengd shouldBe zaak.isVerlengd()
                    isOpgeschort shouldBe zaak.isOpgeschort()
                    isEerderOpgeschort shouldBe zaak.isEerderOpgeschort()
                    indicaties shouldNotContain EnumSet.of(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                }
            }
        }
    }
})
