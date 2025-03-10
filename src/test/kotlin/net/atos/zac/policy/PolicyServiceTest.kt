/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.opa.model.RuleQuery
import net.atos.client.opa.model.RuleResponse
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.client.zgw.drc.model.generated.SoortEnum
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.enkelvoudiginformatieobject.model.createEnkelvoudigInformatieObjectLock
import net.atos.zac.policy.input.DocumentInput
import net.atos.zac.policy.input.UserInput
import net.atos.zac.policy.input.ZaakInput
import net.atos.zac.policy.output.createDocumentRechten
import net.atos.zac.policy.output.createWerklijstRechten
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.createZaakZoekObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createVerlenging
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class PolicyServiceTest : BehaviorSpec({
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val opaEvaluationClient = mockk<OPAEvaluationClient>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val loggedInUser = createLoggedInUser()

    val policyService = PolicyService(
        loggedInUserInstance,
        opaEvaluationClient,
        ztcClientService,
        enkelvoudigInformatieObjectLockService,
        zrcClientService
    )

    Given("zaak with no status") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType()
        val expectedZaakRechten = createZaakRechten()

        val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
        every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("policy rights are requested") {
            val zaakRechten = policyService.readZaakRechten(zaak)

            Then("correct ZaakData is sent to OPA") {
                zaakRechten shouldBe expectedZaakRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                }
                with(ruleQuerySlot.captured.input.zaak) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort
                    verlengd shouldBe zaak.isVerlengd
                    besloten shouldBe false
                    intake shouldBe false
                    heropend shouldBe false
                }
            }
        }
    }

    Given("zaak with status") {
        val zaak = createZaak(
            status = URI("https://example.com/${UUID.randomUUID()}")
        )
        val zaakType = createZaakType()
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType()
        val expectedZaakRechten = createZaakRechten()
        val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
        every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("policy rights are requested") {
            val zaakRechten = policyService.readZaakRechten(zaak)

            Then("correct ZaakData is sent to OPA") {
                zaakRechten shouldBe expectedZaakRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                }
                with(ruleQuerySlot.captured.input.zaak) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort
                    verlengd shouldBe zaak.isVerlengd
                    besloten shouldBe false
                    intake shouldBe false
                    heropend shouldBe false
                }
            }
        }
    }

    Given("locked zaak with no status that is now in intake") {
        val zaak = createZaak(
            verlenging = createVerlenging()
        )
        val zaakType = createZaakType()
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType(omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE)
        val expectedZaakRechten = createZaakRechten()
        val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
        every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("policy rights are requested") {
            val zaakRechten = policyService.readZaakRechten(zaak)

            Then("correct ZaakData is sent to OPA") {
                zaakRechten shouldBe expectedZaakRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                }
                with(ruleQuerySlot.captured.input.zaak) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort
                    verlengd shouldBe zaak.isVerlengd
                    besloten shouldBe false
                    intake shouldBe false
                    heropend shouldBe false
                }
            }
        }
    }

    Given("zaak with status that was reopened") {
        val zaak = createZaak(
            verlenging = createVerlenging(),
            status = URI("https://example.com/${UUID.randomUUID()}")
        )
        val zaakType = createZaakType()
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType(omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND)
        val expectedZaakRechten = createZaakRechten()
        val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
        every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("policy rights are requested") {
            val zaakRechten = policyService.readZaakRechten(zaak)

            Then("correct ZaakData is sent to OPA") {
                zaakRechten shouldBe expectedZaakRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                }
                with(ruleQuerySlot.captured.input.zaak) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort
                    verlengd shouldBe zaak.isVerlengd
                    besloten shouldBe false
                    intake shouldBe false
                    heropend shouldBe true
                }
            }
        }
    }

    Given("ZaakZoekObject") {
        val zaakZoekObject = createZaakZoekObject().apply {
            this.setIndicatie(ZaakIndicatie.OPSCHORTING, true)
            this.setIndicatie(ZaakIndicatie.VERLENGD, true)
            this.setIndicatie(ZaakIndicatie.HEROPEND, true)
        }
        val expectedZaakRechten = createZaakRechten()
        val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()
        every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("policy rights are requested") {
            val zaakRechten = policyService.readZaakRechten(zaakZoekObject)

            Then("correct ZaakData is sent to OPA") {
                zaakRechten shouldBe expectedZaakRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                }
                with(ruleQuerySlot.captured.input.zaak) {
                    open shouldBe true
                    zaaktype shouldBe zaakZoekObject.zaaktypeOmschrijving
                    opgeschort shouldBe true
                    verlengd shouldBe true
                    heropend shouldBe true
                    // We don't set these two
                    besloten shouldBe false
                    intake shouldBe false
                }
            }
        }
    }

    Given("an evaluation client") {
        val expectedWerklijstRechten = createWerklijstRechten()
        val ruleQuerySlot = slot<RuleQuery<UserInput>>()
        every {
            opaEvaluationClient.readWerklijstRechten(capture(ruleQuerySlot))
        } returns RuleResponse(expectedWerklijstRechten)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("the werklijst rechten are requested") {

            val werklijstRechten = policyService.readWerklijstRechten()

            Then("the evaluation client is called with the correct arguments") {
                werklijstRechten shouldBe expectedWerklijstRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readWerklijstRechten(any<RuleQuery<UserInput>>())
                }
                with(ruleQuerySlot.captured.input.user) {
                    id shouldBe loggedInUser.id
                    rollen shouldBe loggedInUser.roles
                    zaaktypen shouldBe loggedInUser.geautoriseerdeZaaktypen
                }
            }
        }
    }

    Given("unsigned information object") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
        val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
        val expectedDocumentRights = createDocumentRechten()
        val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
            expectedDocumentRights
        )
        every { loggedInUserInstance.get() } returns loggedInUser

        When("document policy rights are requested") {
            val documentRights = policyService.readDocumentRechten(
                enkelvoudigInformatieobject,
                enkelvoudigInformatieObjectLock,
                zaak
            )

            Then("the correct data is sent to OPA") {
                documentRights shouldBe expectedDocumentRights

                verify(exactly = 1) {
                    opaEvaluationClient.readDocumentRechten(any<RuleQuery<DocumentInput>>())
                }
                with(ruleQuerySlot.captured.input.documentData) {
                    definitief shouldBe false
                    vergrendeld shouldBe false
                    ondertekend shouldBe false
                    vergrendeldDoor shouldBe null
                    zaaktype shouldBe zaakType.omschrijving
                    zaakOpen shouldBe true
                }
            }
        }
    }

    Given("signed and locked information object") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject(locked = true).apply {
            ondertekening = Ondertekening().apply {
                soort = SoortEnum.ANALOOG
                datum = LocalDate.now()
            }
        }
        val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
        val expectedDocumentRights = createDocumentRechten()
        val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()

        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every {
            opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot))
        } returns RuleResponse(expectedDocumentRights)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("document policy rights are requested") {
            val documentRights = policyService.readDocumentRechten(
                enkelvoudigInformatieobject,
                enkelvoudigInformatieObjectLock,
                zaak
            )

            Then("the correct data is sent to OPA") {
                documentRights shouldBe expectedDocumentRights

                verify(exactly = 1) {
                    opaEvaluationClient.readDocumentRechten(any<RuleQuery<DocumentInput>>())
                }
                with(ruleQuerySlot.captured.input.documentData) {
                    definitief shouldBe false
                    vergrendeld shouldBe true
                    ondertekend shouldBe true
                    vergrendeldDoor shouldBe null
                    zaaktype shouldBe zaakType.omschrijving
                    zaakOpen shouldBe true
                }
            }
        }
    }
})
