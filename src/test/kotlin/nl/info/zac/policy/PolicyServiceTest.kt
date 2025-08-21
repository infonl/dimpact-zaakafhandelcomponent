/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.opa.model.RuleQuery
import nl.info.client.opa.model.RuleResponse
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.Ondertekening
import nl.info.client.zgw.drc.model.generated.SoortEnum
import nl.info.client.zgw.model.createVerlenging
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.model.createEnkelvoudigInformatieObjectLock
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.createZaakZoekObject
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class PolicyServiceTest : BehaviorSpec({
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val opaEvaluationClient = mockk<OpaEvaluationClient>()
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
        val zaak = createZaak(
            status = URI("https://example.com/status/${UUID.randomUUID()}")
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
                with(ruleQuerySlot.captured.input.zaakData) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort()
                    verlengd shouldBe zaak.isVerlengd()
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
                with(ruleQuerySlot.captured.input.zaakData) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort()
                    verlengd shouldBe zaak.isVerlengd()
                    besloten shouldBe false
                    intake shouldBe false
                    heropend shouldBe false
                }
            }
        }
    }

    Given("locked zaak with no status that is now in intake") {
        val zaak = createZaak(
            verlenging = createVerlenging(),
            status = URI("https://example.com/status/${UUID.randomUUID()}"),
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
                with(ruleQuerySlot.captured.input.zaakData) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort()
                    verlengd shouldBe zaak.isVerlengd()
                    besloten shouldBe false
                    intake shouldBe true
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
                with(ruleQuerySlot.captured.input.zaakData) {
                    open shouldBe true
                    zaaktype shouldBe zaakType.omschrijving
                    opgeschort shouldBe zaak.isOpgeschort()
                    verlengd shouldBe zaak.isVerlengd()
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
            val zaakRechten = policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)

            Then("correct ZaakData is sent to OPA") {
                zaakRechten shouldBe expectedZaakRechten
                verify(exactly = 1) {
                    opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                }
                with(ruleQuerySlot.captured.input.zaakData) {
                    open shouldBe true
                    zaaktype shouldBe zaakZoekObject.zaaktypeOmschrijving
                    opgeschort shouldBe true
                    verlengd shouldBe true
                    heropend shouldBe true
                    // We don't set these two
                    besloten shouldBe null
                    intake shouldBe null
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

    Given("readOverigeRechten with zaaktype present in pabcMappings -> rollen from PABC, zaaktypen is single") {
        val zaaktype = "test-zaaktype1"
        val pabcRolesForZaakType = setOf("applicationRole1", "applicationRole2")
        val functionalRoles = setOf("fakeRole1", "fakeRole2")

        val loggedInUserWithMappings = LoggedInUser(
            id = "user1",
            firstName = "Given",
            lastName = "Family",
            displayName = "Full Name",
            email = "user@example.com",
            roles = functionalRoles,
            groupIds = emptySet(),
            geautoriseerdeZaaktypen = setOf("zaakType1", "zaakType2"),
            applicationRolesPerZaaktype = mapOf(zaaktype to pabcRolesForZaakType)
        )

        val rqSlot = slot<RuleQuery<UserInput>>()
        val expected = createOverigeRechten()
        every { loggedInUserInstance.get() } returns loggedInUserWithMappings
        every { opaEvaluationClient.readOverigeRechten(capture(rqSlot)) } returns RuleResponse(expected)

        When("calling readOverigeRechten with a zaaktype") {
            val actual = policyService.readOverigeRechten(zaaktype)

            Then("OPA receives rollen from PABC for that zaaktype and zaaktypen contains only that zaaktype") {
                actual shouldBe expected

                verify(exactly = 1) { opaEvaluationClient.readOverigeRechten(any()) }

                val userData = rqSlot.captured.input.user
                userData.id shouldBe loggedInUserWithMappings.id
                userData.rollen shouldBe pabcRolesForZaakType
                userData.zaaktypen shouldBe setOf(zaaktype)
            }
        }
    }

    Given("readOverigeRechten with null zaaktype - use functional roles + existing zaaktypen") {
        val functionalRoles = setOf("fakeRole1", "fakeRole2")
        val geautoriseerde = setOf("zaaktype1", "zaaktype2")
        val loggedInUserLegacy = LoggedInUser(
            id = "user1",
            firstName = null,
            lastName = null,
            displayName = null,
            email = null,
            roles = functionalRoles,
            groupIds = emptySet(),
            geautoriseerdeZaaktypen = geautoriseerde,
            applicationRolesPerZaaktype = emptyMap()
        )

        val rqSlot = slot<RuleQuery<UserInput>>()
        val expected = createOverigeRechten()
        every { loggedInUserInstance.get() } returns loggedInUserLegacy
        every { opaEvaluationClient.readOverigeRechten(capture(rqSlot)) } returns RuleResponse(expected)

        When("calling readOverigeRechten with null") {
            val actual = policyService.readOverigeRechten(null)

            Then("OPA receives functional roles and original geautoriseerde zaaktypen") {
                actual shouldBe expected

                verify(exactly = 1) { opaEvaluationClient.readOverigeRechten(any()) }

                val userData = rqSlot.captured.input.user
                userData.rollen shouldBe functionalRoles
                userData.zaaktypen shouldBe geautoriseerde
            }
        }
    }
})
