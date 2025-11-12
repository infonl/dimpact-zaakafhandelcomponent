/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
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
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.model.createEnkelvoudigInformatieObjectLock
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createTaakRechten
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
    val configuratieService = mockk<ConfiguratieService>()
    val loggedInUser = createLoggedInUser()
    val policyService = PolicyService(
        loggedInUserInstance,
        opaEvaluationClient,
        ztcClientService,
        enkelvoudigInformatieObjectLockService,
        zrcClientService,
        configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Reading zaakrechten") {
        Given(
            """
            A logged-in with functional roles, application roles per zaaktype mappings, 
            and a zaak, with PABC feature flag enabled
            """
        ) {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val applicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val zaak = createZaak(
                status = URI("https://example.com/status/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType(
                omschrijving = zaaktypeOmschrijving
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()
            val loggedInUser = createLoggedInUser(
                roles = setOf("fakeRole1", "fakeRole2"),
                // obsolete and not used when PABC feature flag is enabled
                geautoriseerdeZaaktypen = null,
                applicationRolesPerZaaktype = setOf(
                    zaaktypeOmschrijving to applicationRolesForZaakType,
                    "fakeZaaktype2" to setOf("fakeApplicationRole3")
                ).toMap()
            )

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configuratieService.featureFlagPabcIntegration() } returns true

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("the returned zaakrechten are correct") {
                    zaakRechten shouldBe expectedZaakRechten
                }

                And("the expected evaluation data is sent to the policy evaluation client") {
                    verify(exactly = 1) {
                        opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    val zaakInput = ruleQuerySlot.captured.input
                    with(zaakInput) {
                        featureFlagPabcIntegration shouldBe true
                    }
                    with(zaakInput.zaakData) {
                        open shouldBe true
                        zaaktype shouldBe zaakType.omschrijving
                        opgeschort shouldBe zaak.isOpgeschort()
                        verlengd shouldBe zaak.isVerlengd()
                        besloten shouldBe false
                        intake shouldBe false
                        heropend shouldBe false
                    }
                    with(zaakInput.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactly applicationRolesForZaakType
                        zaaktypen shouldBe setOf(zaakType.omschrijving)
                    }
                }
            }
        }

        Given("A logged-in user, a zaak and PABC feature flag disabled") {
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
            every { configuratieService.featureFlagPabcIntegration() } returns false

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

        Given("locked zaak that has intake status and PABC feature flag enabled") {
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
            every { configuratieService.featureFlagPabcIntegration() } returns true

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

        Given("zaak with status that was reopened and PABC feature flag enabled") {
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
            every { configuratieService.featureFlagPabcIntegration() } returns true

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
    }

    Context("Reading zaakrechten for ZaakZoekObject") {
        Given("ZaakZoekObject and PABC feature flag enabled") {
            val zaakZoekObject = createZaakZoekObject().apply {
                this.setIndicatie(ZaakIndicatie.OPSCHORTING, true)
                this.setIndicatie(ZaakIndicatie.VERLENGD, true)
                this.setIndicatie(ZaakIndicatie.HEROPEND, true)
            }
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configuratieService.featureFlagPabcIntegration() } returns true

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
    }

    Context("Reading taakrechten") {
        Given(
            """
            An open CMMN task as part of a zaak and a logged in user with application roles for the zaaktype of the zaak
            and PABC feature flag enabled
            """
        ) {
            val zaakType = createZaakType()
            val testTask = createTestTask(
                caseVariables = mapOf("zaaktypeOmschrijving" to zaakType.omschrijving)
            )
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val expectedTaakRechten = createTaakRechten()
            val ruleQuerySlot = slot<RuleQuery<TaakInput>>()

            every { opaEvaluationClient.readTaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedTaakRechten
            )
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configuratieService.featureFlagPabcIntegration() } returns true

            When("task policy rights are requested for the task") {
                val taskPermissions = policyService.readTaakRechten(testTask)

                Then("the response contains the expected taakrechten") {
                    taskPermissions shouldBe expectedTaakRechten
                }
                And("the correct data is sent to the OPA evaluation client") {
                    verify(exactly = 1) {
                        opaEvaluationClient.readTaakRechten(any<RuleQuery<TaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.taakData) {
                        open shouldBe true
                        zaaktype shouldBe zaakType.omschrijving
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }
    }

    Context("Reading werklijstrechten") {
        Given("A logged-in user with functional roles, roles mappings and PABC feature flag enabled") {
            val expectedWerklijstRechten = createWerklijstRechten()
            val ruleQuerySlot = slot<RuleQuery<UserInput>>()
            val zaaktype1Omschrijving = "fakeZaaktype1"
            val zaaktype2Omschrijving = "fakeZaaktype2"
            val applicationRolesForZaakType1 = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val applicationRolesForZaakType2 = setOf("fakeApplicationRole3")
            val loggedInUser = createLoggedInUser(
                roles = setOf("fakeRole1", "fakeRole2"),
                // obsolete and not used when PABC feature flag is enabled
                geautoriseerdeZaaktypen = null,
                applicationRolesPerZaaktype = setOf(
                    zaaktype1Omschrijving to applicationRolesForZaakType1,
                    zaaktype2Omschrijving to applicationRolesForZaakType2
                ).toMap()
            )
            every {
                opaEvaluationClient.readWerklijstRechten(capture(ruleQuerySlot))
            } returns RuleResponse(expectedWerklijstRechten)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configuratieService.featureFlagPabcIntegration() } returns true

            When("the werklijst rechten are requested") {
                val werklijstRechten = policyService.readWerklijstRechten()

                Then("the evaluation client is called with the correct arguments") {
                    werklijstRechten shouldBe expectedWerklijstRechten
                    verify(exactly = 1) {
                        opaEvaluationClient.readWerklijstRechten(any<RuleQuery<UserInput>>())
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        // this policy check is not zaaktype-specific,
                        // so the roles should be the union of all application roles for which at least one zaaktype is authorized
                        rollen shouldContainExactly applicationRolesForZaakType1 + applicationRolesForZaakType2
                        // this policy check is not zaaktype-specific, so zaaktypen should be null
                        zaaktypen shouldBe null
                    }
                }
            }
        }

        Given("A logged-in user and PABC feature flag disabled") {
            val expectedWerklijstRechten = createWerklijstRechten()
            val ruleQuerySlot = slot<RuleQuery<UserInput>>()
            every {
                opaEvaluationClient.readWerklijstRechten(capture(ruleQuerySlot))
            } returns RuleResponse(expectedWerklijstRechten)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configuratieService.featureFlagPabcIntegration() } returns false

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
    }

    Context("Reading documentrechten") {
        Given("Unsigned information object and PABC feature flag enabled") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedDocumentRights
            )
            every { loggedInUserInstance.get() } returns loggedInUser
            every { configuratieService.featureFlagPabcIntegration() } returns true

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
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        Given("signed and locked information object and PABC feature flag enabled") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
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
            every { configuratieService.featureFlagPabcIntegration() } returns true

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
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        Given("Unsigned information object and PABC feature flag disabled") {
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
            every { configuratieService.featureFlagPabcIntegration() } returns false

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
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldBe loggedInUser.roles
                        zaaktypen shouldBe loggedInUser.geautoriseerdeZaaktypen
                    }
                }
            }
        }

        Given("signed and locked information object and PABC feature flag disabled") {
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
            every { configuratieService.featureFlagPabcIntegration() } returns false

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
    }

    Context("Reading overige rechten") {
        val functionalRoles = setOf("fakeRole1", "fakeRole2")

        Given("A logged-in user with application roles per zaaktype with PABC integration enabled") {
            val zaaktype = "test-zaaktype"
            val pabcRolesForZaakType = setOf("applicationRole1", "applicationRole2")
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
            every { configuratieService.featureFlagPabcIntegration() } returns true

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

        Given("A logged-in user with authorized zaaktypes without PABC integration enabled") {
            val roles = setOf("fakeRole1", "fakeRole2")
            val authorizedZaaktypes = setOf("zaaktype1", "zaaktype2")
            val loggedInUserLegacy = LoggedInUser(
                id = "user1",
                firstName = null,
                lastName = null,
                displayName = null,
                email = null,
                roles = roles,
                groupIds = emptySet(),
                geautoriseerdeZaaktypen = authorizedZaaktypes,
                applicationRolesPerZaaktype = emptyMap()
            )

            val rqSlot = slot<RuleQuery<UserInput>>()
            val expected = createOverigeRechten()
            every { loggedInUserInstance.get() } returns loggedInUserLegacy
            every { opaEvaluationClient.readOverigeRechten(capture(rqSlot)) } returns RuleResponse(expected)
            every { configuratieService.featureFlagPabcIntegration() } returns false

            When("calling readOverigeRechten without a zaaktype") {
                val actual = policyService.readOverigeRechten(null)

                Then("OPA receives functional roles and original geautoriseerde zaaktypen") {
                    actual shouldBe expected

                    verify(exactly = 1) { opaEvaluationClient.readOverigeRechten(any()) }

                    val userData = rqSlot.captured.input.user
                    userData.rollen shouldBe roles
                    userData.zaaktypen shouldBe authorizedZaaktypes
                }
            }

            When("calling readOverigeRechten with a zaaktype") {
                clearMocks(opaEvaluationClient, answers = false, recordedCalls = true, exclusionRules = false)
                val actual = policyService.readOverigeRechten("redundant-zaaktype")

                Then("OPA receives roles with the authorized zaaktypes") {
                    actual shouldBe expected

                    verify(exactly = 1) { opaEvaluationClient.readOverigeRechten(any()) }

                    val userData = rqSlot.captured.input.user
                    userData.rollen shouldBe roles
                    userData.zaaktypen shouldBe authorizedZaaktypes
                }
            }
        }
    }
})
