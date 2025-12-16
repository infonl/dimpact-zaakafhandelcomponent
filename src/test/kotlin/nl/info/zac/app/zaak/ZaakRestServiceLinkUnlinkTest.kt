/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.AardRelatieEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.createRestZaakLinkData
import nl.info.zac.app.zaak.model.createRestZaakUnlinkData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import java.util.UUID

@Suppress("LongParameterList", "LargeClass")
class ZaakRestServiceLinkUnlinkTest : BehaviorSpec({
    val decisionService = mockk<DecisionService>()
    val bpmnService = mockk<BpmnService>()
    val brcClientService = mockk<BrcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val healthCheckService = mockk<HealthCheckService>()
    val identityService = mockk<IdentityService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val objectsClientService = mockk<ObjectsClientService>()
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val opschortenZaakHelper = mockk<SuspensionZaakHelper>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val restDecisionConverter = mockk<RestDecisionConverter>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val zaakHistoryLineConverter = mockk<ZaakHistoryLineConverter>()
    val signaleringService = mockk<SignaleringService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakHistoryService = mockk<ZaakHistoryService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakRestService = ZaakRestService(
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        cmmnService = cmmnService,
        configuratieService = configuratieService,
        decisionService = decisionService,
        dispatcher = testDispatcher,
        drcClientService = drcClientService,
        eventingService = eventingService,
        flowableTaskService = flowableTaskService,
        healthCheckService = healthCheckService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        indexingService = indexingService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        opschortenZaakHelper = opschortenZaakHelper,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        restDecisionConverter = restDecisionConverter,
        restZaakConverter = restZaakConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        signaleringService = signaleringService,
        zaakHistoryLineConverter = zaakHistoryLineConverter,
        zaakHistoryService = zaakHistoryService,
        zaakService = zaakService,
        zaakVariabelenService = zaakVariabelenService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Linking a zaak") {
        Given("Two open zaken with zaak link data using a 'bijdrage' relatie and in reverse an 'onderwerp' relatie") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.BIJDRAGE,
                reverseRelatieType = RelatieType.ONDERWERP
            )
            val patchZaakUUIDSlot = mutableListOf<UUID>()
            val patchZaakSlot = mutableListOf<Zaak>()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType) } returns createZaakRechten()
            every { zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot)) } returns zaak

            When("the zaken are linked") {
                zaakRestService.linkZaak(restZaakLinkData)

                Then("the two zaken are successfully linked") {
                    verify(exactly = 2) {
                        zrcClientService.patchZaak(any(), any())
                    }
                    patchZaakUUIDSlot[0] shouldBe zaak.uuid
                    patchZaakUUIDSlot[1] shouldBe teKoppelenZaak.uuid
                    with(patchZaakSlot[0]) {
                        relevanteAndereZaken shouldHaveSize (1)
                        with(relevanteAndereZaken[0]) {
                            url shouldBe teKoppelenZaak.url
                            aardRelatie shouldBe AardRelatieEnum.BIJDRAGE
                        }
                    }
                    with(patchZaakSlot[1]) {
                        relevanteAndereZaken shouldHaveSize (1)
                        with(relevanteAndereZaken[0]) {
                            url shouldBe zaak.url
                            aardRelatie shouldBe AardRelatieEnum.ONDERWERP
                        }
                    }
                }
            }
        }

        Given("Two open zaken with zaak link data using a 'hoofdzaak' relatie and no reverse relation") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.HOOFDZAAK
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType) } returns createZaakRechten()
            every { zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot)) } returns zaak
            every { indexingService.addOrUpdateZaak(teKoppelenZaak.uuid, false) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("the zaken are linked") {
                zaakRestService.linkZaak(restZaakLinkData)

                Then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                    with(patchZaakSlot.captured) {
                        hoofdzaak shouldBe teKoppelenZaak.url
                    }
                }
            }
        }

        Given("An open zaak and a closed zaak") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.HOOFDZAAK
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType) } returns createZaakRechten()

            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot))
            } returns zaak
            every { indexingService.addOrUpdateZaak(teKoppelenZaak.uuid, false) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("the zaken are linked") {
                zaakRestService.linkZaak(restZaakLinkData)

                Then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                    with(patchZaakSlot.captured) {
                        hoofdzaak shouldBe teKoppelenZaak.url
                    }
                }
            }
        }
    }

    Context("Unlinking a zaak") {
        Given("Two linked zaken with the relation 'vervolg'") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.VERVOLG,
                reason = "fakeUnlinkReason"
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType) } returns createZaakRechten()
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot), "fakeUnlinkReason")
            } returns zaak

            When("the zaken are unlinked") {
                zaakRestService.unlinkZaak(restZaakUnlinkData)

                Then("the two zaken are successfully unlinked") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                    with(patchZaakSlot.captured) {
                        relevanteAndereZaken shouldBe null
                    }
                }
            }
        }
    }
})
