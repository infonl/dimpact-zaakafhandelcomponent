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
import nl.info.client.zgw.model.createZaakEigenschap
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
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.createEnkelvoudigInformatieObjectLock
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
import nl.info.zac.search.model.createDocumentZoekObject
import nl.info.zac.search.model.createTaakZoekObject
import nl.info.zac.search.model.createZaakZoekObject
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LargeClass")
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

    afterEach {
        checkUnnecessaryStub()
    }

    context("Reading zaakrechten") {
        given(
            """
            A logged-in with functional roles, application roles per zaaktype mappings, 
            and a zaak
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
                applicationRolesPerZaaktype = setOf(
                    zaaktypeOmschrijving to applicationRolesForZaakType,
                    "fakeZaaktype2" to setOf("fakeApplicationRole3")
                ).toMap()
            )

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            `when`("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak, loggedInUser)

                then("the returned zaakrechten are correct") {
                    zaakRechten shouldBe expectedZaakRechten
                }

                and("the expected evaluation data is sent to the policy evaluation client") {
                    verify(exactly = 1) {
                        opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    val zaakInput = ruleQuerySlot.captured.input
                    with(zaakInput.zaakData) {
                        open shouldBe true
                        zaaktype shouldBe zaakType.omschrijving
                        opgeschort shouldBe zaak.isOpgeschort()
                        verlengd shouldBe zaak.isVerlengd()
                        besloten shouldBe false
                        intake shouldBe false
                        heropend shouldBe false
                        brondatumBepaald shouldBe false
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                    with(zaakInput.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactly applicationRolesForZaakType
                        zaaktypen shouldBe setOf(zaakType.omschrijving)
                    }
                }
            }
        }

        given("a zaak for which the brondatum has been determined") {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val zaak = createZaak(
                status = URI("https://example.com/status/${UUID.randomUUID()}")
            ).apply {
                startdatumBewaartermijn = LocalDate.now()
            }
            val zaakType = createZaakType(
                omschrijving = zaaktypeOmschrijving
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            `when`("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak, loggedInUser)

                then("the returned zaakrechten are correct and brondatumBepaald is true") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        opaEvaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    ruleQuerySlot.captured.input.zaakData.brondatumBepaald shouldBe true
                }
            }
        }

        given("locked zaak that has intake status") {
            val zaak = createZaak(
                verlenging = createVerlenging(),
                status = URI("https://example.com/status/${UUID.randomUUID()}"),
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_INTAKE)
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            `when`("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak, loggedInUser)

                then("correct ZaakData is sent to OPA") {
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
                        brondatumBepaald shouldBe false
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                }
            }
        }

        given("zaak with status that was reopened") {
            val zaak = createZaak(
                verlenging = createVerlenging(),
                status = URI("https://example.com/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_HEROPEND)
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            `when`("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak, loggedInUser)

                then("correct ZaakData is sent to OPA") {
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
                        brondatumBepaald shouldBe false
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                }
            }
        }

        given("a zaak marked as zaakspecifiek geautoriseerd") {
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
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
            )
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            `when`("policy rights are requested") {
                policyService.readZaakRechten(zaak, loggedInUser)

                then("zaakspecifiekGeautoriseerd is true in the ZaakData sent to OPA") {
                    ruleQuerySlot.captured.input.zaakData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }
    }

    context("Reading zaakrechten for searching zaken") {
        given("A ZaakZoekObject") {
            val zaakZoekObject = createZaakZoekObject().apply {
                this.setIndicatie(ZaakIndicatie.OPSCHORTING, true)
                this.setIndicatie(ZaakIndicatie.VERLENGD, true)
                this.setIndicatie(ZaakIndicatie.HEROPEND, true)
            }
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            `when`("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)

                then("correct ZaakData is sent to OPA") {
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
                        // We don't set these three
                        besloten shouldBe null
                        intake shouldBe null
                        brondatumBepaald shouldBe null
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                }
            }
        }

        given("A ZaakZoekObject representing a zaakspecifiek geautoriseerde zaak") {
            val zaakZoekObject = createZaakZoekObject(isZaakspecifiekGeautoriseerd = true)
            val expectedZaakRechten = createZaakRechten()
            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()
            every { opaEvaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            `when`("policy rights are requested") {
                policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)

                then("zaakspecifiekGeautoriseerd is true in the ZaakData sent to OPA") {
                    ruleQuerySlot.captured.input.zaakData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }
    }

    context("Reading taakrechten") {
        given(
            """
            An open CMMN task as part of a zaak and a logged in user with application roles for the zaaktype of the zaak            
            """
        ) {
            val zaakType = createZaakType()
            val zaakUUID = UUID.randomUUID()
            val testTask = createTestTask(
                caseVariables = mapOf(
                    "zaaktypeOmschrijving" to zaakType.omschrijving,
                    "zaakUUID" to zaakUUID
                )
            )
            val userApplicationRolesForZaakType = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to userApplicationRolesForZaakType
                )
            )
            val expectedTaakRechten = createTaakRechten()
            val ruleQuerySlot = slot<RuleQuery<TaakInput>>()

            every { zrcClientService.listZaakeigenschappen(zaakUUID) } returns emptyList()
            every { opaEvaluationClient.readTaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedTaakRechten
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("task policy rights are requested for the task") {
                val taskPermissions = policyService.readTaakRechten(testTask)

                then("the response contains the expected taakrechten") {
                    taskPermissions shouldBe expectedTaakRechten
                }
                and("the correct data is sent to the OPA evaluation client") {
                    verify(exactly = 1) {
                        opaEvaluationClient.readTaakRechten(any<RuleQuery<TaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.taakData) {
                        open shouldBe true
                        zaaktype shouldBe zaakType.omschrijving
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        given("an open CMMN task that is part of a zaak marked as zaakspecifiek geautoriseerd") {
            val zaakType = createZaakType()
            val zaakUUID = UUID.randomUUID()
            val testTask = createTestTask(
                caseVariables = mapOf(
                    "zaaktypeOmschrijving" to zaakType.omschrijving,
                    "zaakUUID" to zaakUUID
                )
            )
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to setOf("fakeApplicationRole1")
                )
            )
            val expectedTaakRechten = createTaakRechten()
            val ruleQuerySlot = slot<RuleQuery<TaakInput>>()

            every { zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
                createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
            )
            every { opaEvaluationClient.readTaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedTaakRechten
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("task policy rights are requested for the task") {
                policyService.readTaakRechten(testTask)

                then("zaakspecifiekGeautoriseerd is true in the TaakData sent to OPA") {
                    ruleQuerySlot.captured.input.taakData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }
    }

    context("Reading taakrechten for searching tasks") {
        given(
            """
            An open CMMN task as part of a zaak and a logged in user with application roles for the zaaktype of the zaak           
            """
        ) {
            val zaakType = createZaakType()
            val taakZoekObject = createTaakZoekObject(
                zaaktypeOmschrijving = zaakType.omschrijving
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

            `when`("task policy rights are requested for the task search object") {
                val taskPermissions = policyService.readTaakRechten(taakZoekObject)

                then("the response contains the expected taakrechten") {
                    taskPermissions shouldBe expectedTaakRechten
                }
                and("the correct data is sent to the OPA evaluation client") {
                    verify(exactly = 1) {
                        opaEvaluationClient.readTaakRechten(any<RuleQuery<TaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.taakData) {
                        // 'open' is always false for task search objects
                        open shouldBe false
                        zaaktype shouldBe zaakType.omschrijving
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        given("A TaakZoekObject whose associated zaak is zaakspecifiek geautoriseerd") {
            val taakZoekObject = createTaakZoekObject(isZaakspecifiekGeautoriseerd = true)
            val expectedTaakRechten = createTaakRechten()
            val ruleQuerySlot = slot<RuleQuery<TaakInput>>()

            every { opaEvaluationClient.readTaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedTaakRechten
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            `when`("task policy rights are requested for the task search object") {
                policyService.readTaakRechten(taakZoekObject)

                then("zaakspecifiekGeautoriseerd is true in the TaakData sent to OPA") {
                    ruleQuerySlot.captured.input.taakData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }
    }

    context("Reading werklijstrechten") {
        given("A logged-in user with functional roles, roles mappings") {
            val expectedWerklijstRechten = createWerklijstRechten()
            val ruleQuerySlot = slot<RuleQuery<UserInput>>()
            val zaaktype1Omschrijving = "fakeZaaktype1"
            val zaaktype2Omschrijving = "fakeZaaktype2"
            val applicationRolesForZaakType1 = setOf("fakeApplicationRole1", "fakeApplicationRole2")
            val applicationRolesForZaakType2 = setOf("fakeApplicationRole3")
            val loggedInUser = createLoggedInUser(
                roles = setOf("fakeRole1", "fakeRole2"),
                applicationRolesPerZaaktype = setOf(
                    zaaktype1Omschrijving to applicationRolesForZaakType1,
                    zaaktype2Omschrijving to applicationRolesForZaakType2
                ).toMap()
            )
            every {
                opaEvaluationClient.readWerklijstRechten(capture(ruleQuerySlot))
            } returns RuleResponse(expectedWerklijstRechten)
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("the werklijst rechten are requested") {
                val werklijstRechten = policyService.readWerklijstRechten()

                then("the evaluation client is called with the correct arguments") {
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
    }

    context("Reading documentrechten") {
        given("An unsigned information object") {
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
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedDocumentRights
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("document policy rights are requested") {
                val documentRights = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                then("the correct data is sent to OPA") {
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
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        given("signed and locked information object") {
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
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every {
                opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot))
            } returns RuleResponse(expectedDocumentRights)
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("document policy rights are requested") {
                val documentRights = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                then("the correct data is sent to OPA") {
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
                        zaakspecifiekGeautoriseerd shouldBe false
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        id shouldBe loggedInUser.id
                        rollen shouldContainExactlyInAnyOrder userApplicationRolesForZaakType
                        zaaktypen shouldContainExactly listOf(zaakType.omschrijving)
                    }
                }
            }
        }

        given("a document linked to a zaak marked as zaakspecifiek geautoriseerd") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser(
                applicationRolesPerZaaktype = mapOf(
                    zaakType.omschrijving to setOf("fakeApplicationRole1")
                )
            )
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
            )
            every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedDocumentRights
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("document policy rights are requested") {
                policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    zaak
                )

                then("zaakspecifiekGeautoriseerd is true in the DocumentData sent to OPA") {
                    ruleQuerySlot.captured.input.documentData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }

        given("a document that is not linked to any zaak") {
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val enkelvoudigInformatieObjectLock = createEnkelvoudigInformatieObjectLock()
            val expectedDocumentRights = createDocumentRechten()
            val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()
            val loggedInUser = createLoggedInUser()

            every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedDocumentRights
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("document policy rights are requested with no zaak") {
                policyService.readDocumentRechten(
                    enkelvoudigInformatieobject,
                    enkelvoudigInformatieObjectLock,
                    null
                )

                then("zaakspecifiekGeautoriseerd is false in the DocumentData sent to OPA") {
                    ruleQuerySlot.captured.input.documentData.zaakspecifiekGeautoriseerd shouldBe false
                }
            }
        }
    }

    context("Reading documentrechten for searching documents") {
        given("A DocumentZoekObject whose associated zaak is not zaakspecifiek geautoriseerd") {
            val documentZoekObject = createDocumentZoekObject()
            val expectedDocumentRights = createDocumentRechten()
            val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()

            every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedDocumentRights
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            `when`("document policy rights are requested for the document search object") {
                val documentRights = policyService.readDocumentRechten(documentZoekObject)

                then("zaakspecifiekGeautoriseerd is false in the DocumentData sent to OPA") {
                    documentRights shouldBe expectedDocumentRights
                    ruleQuerySlot.captured.input.documentData.zaakspecifiekGeautoriseerd shouldBe false
                }
            }
        }

        given("A DocumentZoekObject whose associated zaak is zaakspecifiek geautoriseerd") {
            val documentZoekObject = createDocumentZoekObject(isZaakspecifiekGeautoriseerd = true)
            val expectedDocumentRights = createDocumentRechten()
            val ruleQuerySlot = slot<RuleQuery<DocumentInput>>()

            every { opaEvaluationClient.readDocumentRechten(capture(ruleQuerySlot)) } returns RuleResponse(
                expectedDocumentRights
            )
            every { loggedInUserInstance.get() } returns createLoggedInUser()

            `when`("document policy rights are requested for the document search object") {
                policyService.readDocumentRechten(documentZoekObject)

                then("zaakspecifiekGeautoriseerd is true in the DocumentData sent to OPA") {
                    ruleQuerySlot.captured.input.documentData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }
    }

    context("Reading overige rechten") {
        val functionalRoles = setOf("fakeRole1", "fakeRole2")

        given("A logged-in user with application roles per zaaktype") {
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
                applicationRolesPerZaaktype = mapOf(zaaktype to pabcRolesForZaakType)
            )

            val rqSlot = slot<RuleQuery<UserInput>>()
            val expected = createOverigeRechten()
            every { loggedInUserInstance.get() } returns loggedInUserWithMappings
            every { opaEvaluationClient.readOverigeRechten(capture(rqSlot)) } returns RuleResponse(expected)

            `when`("calling readOverigeRechten with a zaaktype") {
                val actual = policyService.readOverigeRechten(zaaktype)

                then("OPA receives rollen from PABC for that zaaktype and zaaktypen contains only that zaaktype") {
                    actual shouldBe expected

                    verify(exactly = 1) { opaEvaluationClient.readOverigeRechten(any()) }

                    val userData = rqSlot.captured.input.user
                    userData.id shouldBe loggedInUserWithMappings.id
                    userData.rollen shouldBe pabcRolesForZaakType
                    userData.zaaktypen shouldBe setOf(zaaktype)
                }
            }
        }
    }

    context(
        """
        Parity between zoekobject-based rechten and their equivalent single-resource rechten
        (both mocked OPA responses are the same instance, so the assertion that matters is that
        both call paths send OPA decision-equivalent input for the same underlying zaak/taak/document)
        """
    ) {
        given("a zaak that is not zaakspecifiek geautoriseerd, read as a Zaak and as a ZaakZoekObject") {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val zaak = createZaak(status = URI("https://example.com/status/${UUID.randomUUID()}"))
            val zaakType = createZaakType(omschrijving = zaaktypeOmschrijving)
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val zaakZoekObject = createZaakZoekObject(zaaktypeOmschrijving = zaaktypeOmschrijving)
            val expectedZaakRechten = createZaakRechten()
            val zaakRuleQuerySlot = slot<RuleQuery<ZaakInput>>()
            val zaakZoekObjectRuleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("zaakrechten are read for the zaak and for the equivalent zaak zoek object") {
                every {
                    opaEvaluationClient.readZaakRechten(capture(zaakRuleQuerySlot))
                } returns RuleResponse(expectedZaakRechten)
                val zaakRechten = policyService.readZaakRechten(zaak, loggedInUser)

                every {
                    opaEvaluationClient.readZaakRechten(capture(zaakZoekObjectRuleQuerySlot))
                } returns RuleResponse(expectedZaakRechten)
                val zaakZoekObjectRechten = policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)

                then("both paths send OPA the same decision-relevant ZaakData and return the same rechten") {
                    zaakZoekObjectRechten shouldBe zaakRechten

                    val zaakData = zaakRuleQuerySlot.captured.input.zaakData
                    val zaakZoekObjectData = zaakZoekObjectRuleQuerySlot.captured.input.zaakData
                    // intake, besloten and brondatumBepaald are deliberately excluded: not tracked for
                    // a ZaakZoekObject, unlike a Zaak
                    zaakZoekObjectData.zaaktype shouldBe zaakData.zaaktype
                    zaakZoekObjectData.open shouldBe zaakData.open
                    zaakZoekObjectData.opgeschort shouldBe zaakData.opgeschort
                    zaakZoekObjectData.verlengd shouldBe zaakData.verlengd
                    zaakZoekObjectData.heropend shouldBe zaakData.heropend
                    zaakZoekObjectData.zaakspecifiekGeautoriseerd shouldBe zaakData.zaakspecifiekGeautoriseerd
                    zaakZoekObjectData.zaakspecifiekGeautoriseerd shouldBe false
                }
            }
        }

        given("a zaak that is zaakspecifiek geautoriseerd, read as a Zaak and as a ZaakZoekObject") {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val zaak = createZaak(status = URI("https://example.com/status/${UUID.randomUUID()}"))
            val zaakType = createZaakType(omschrijving = zaaktypeOmschrijving)
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val zaakZoekObject = createZaakZoekObject(
                zaaktypeOmschrijving = zaaktypeOmschrijving,
                isZaakspecifiekGeautoriseerd = true
            )
            val expectedZaakRechten = createZaakRechten()
            val zaakRuleQuerySlot = slot<RuleQuery<ZaakInput>>()
            val zaakZoekObjectRuleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("zaakrechten are read for the zaak and for the equivalent zaak zoek object") {
                every {
                    opaEvaluationClient.readZaakRechten(capture(zaakRuleQuerySlot))
                } returns RuleResponse(expectedZaakRechten)
                val zaakRechten = policyService.readZaakRechten(zaak, loggedInUser)

                every {
                    opaEvaluationClient.readZaakRechten(capture(zaakZoekObjectRuleQuerySlot))
                } returns RuleResponse(expectedZaakRechten)
                val zaakZoekObjectRechten = policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)

                then("both paths send OPA zaakspecifiekGeautoriseerd = true and return the same rechten") {
                    zaakZoekObjectRechten shouldBe zaakRechten
                    zaakRuleQuerySlot.captured.input.zaakData.zaakspecifiekGeautoriseerd shouldBe true
                    zaakZoekObjectRuleQuerySlot.captured.input.zaakData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }

        given("a task that is not part of a zaakspecifiek geautoriseerde zaak, read as a TaskInfo and as a TaakZoekObject") {
            val zaakType = createZaakType()
            val zaakUUID = UUID.randomUUID()
            val testTask = createTestTask(
                caseVariables = mapOf(
                    "zaaktypeOmschrijving" to zaakType.omschrijving,
                    "zaakUUID" to zaakUUID
                )
            )
            val taakZoekObject = createTaakZoekObject(zaaktypeOmschrijving = zaakType.omschrijving)
            val expectedTaakRechten = createTaakRechten()
            val taskInfoRuleQuerySlot = slot<RuleQuery<TaakInput>>()
            val taakZoekObjectRuleQuerySlot = slot<RuleQuery<TaakInput>>()

            every { zrcClientService.listZaakeigenschappen(zaakUUID) } returns emptyList()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("taakrechten are read for the task and for the equivalent taak zoek object") {
                every {
                    opaEvaluationClient.readTaakRechten(capture(taskInfoRuleQuerySlot))
                } returns RuleResponse(expectedTaakRechten)
                val taskInfoRechten = policyService.readTaakRechten(testTask)

                every {
                    opaEvaluationClient.readTaakRechten(capture(taakZoekObjectRuleQuerySlot))
                } returns RuleResponse(expectedTaakRechten)
                val taakZoekObjectRechten = policyService.readTaakRechten(taakZoekObject)

                then("both paths send OPA the same decision-relevant TaakData and return the same rechten") {
                    taakZoekObjectRechten shouldBe taskInfoRechten

                    val taskInfoData = taskInfoRuleQuerySlot.captured.input.taakData
                    val taakZoekObjectData = taakZoekObjectRuleQuerySlot.captured.input.taakData
                    // 'open' is deliberately excluded: a TaakZoekObject does not track it, unlike a TaskInfo
                    taakZoekObjectData.zaaktype shouldBe taskInfoData.zaaktype
                    taakZoekObjectData.zaakspecifiekGeautoriseerd shouldBe taskInfoData.zaakspecifiekGeautoriseerd
                    taakZoekObjectData.zaakspecifiekGeautoriseerd shouldBe false
                }
            }
        }

        given("a task that is part of a zaakspecifiek geautoriseerde zaak, read as a TaskInfo and as a TaakZoekObject") {
            val zaakType = createZaakType()
            val zaakUUID = UUID.randomUUID()
            val testTask = createTestTask(
                caseVariables = mapOf(
                    "zaaktypeOmschrijving" to zaakType.omschrijving,
                    "zaakUUID" to zaakUUID
                )
            )
            val taakZoekObject = createTaakZoekObject(
                zaaktypeOmschrijving = zaakType.omschrijving,
                isZaakspecifiekGeautoriseerd = true
            )
            val expectedTaakRechten = createTaakRechten()
            val taskInfoRuleQuerySlot = slot<RuleQuery<TaakInput>>()
            val taakZoekObjectRuleQuerySlot = slot<RuleQuery<TaakInput>>()

            every { zrcClientService.listZaakeigenschappen(zaakUUID) } returns listOf(
                createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("taakrechten are read for the task and for the equivalent taak zoek object") {
                every {
                    opaEvaluationClient.readTaakRechten(capture(taskInfoRuleQuerySlot))
                } returns RuleResponse(expectedTaakRechten)
                val taskInfoRechten = policyService.readTaakRechten(testTask)

                every {
                    opaEvaluationClient.readTaakRechten(capture(taakZoekObjectRuleQuerySlot))
                } returns RuleResponse(expectedTaakRechten)
                val taakZoekObjectRechten = policyService.readTaakRechten(taakZoekObject)

                then("both paths send OPA zaakspecifiekGeautoriseerd = true and return the same rechten") {
                    taakZoekObjectRechten shouldBe taskInfoRechten
                    taskInfoRuleQuerySlot.captured.input.taakData.zaakspecifiekGeautoriseerd shouldBe true
                    taakZoekObjectRuleQuerySlot.captured.input.taakData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }

        given(
            "a document linked to a zaak that is not zaakspecifiek geautoriseerd, read as an " +
                "EnkelvoudigInformatieObject and as a DocumentZoekObject"
        ) {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val zaak = createZaak()
            val zaakType = createZaakType(omschrijving = zaaktypeOmschrijving)
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val documentZoekObject = createDocumentZoekObject(zaaktypeOmschrijving = zaaktypeOmschrijving)
            val expectedDocumentRechten = createDocumentRechten()
            val enkelvoudigInformatieobjectRuleQuerySlot = slot<RuleQuery<DocumentInput>>()
            val documentZoekObjectRuleQuerySlot = slot<RuleQuery<DocumentInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("documentrechten are read for the document and for the equivalent document zoek object") {
                every {
                    opaEvaluationClient.readDocumentRechten(capture(enkelvoudigInformatieobjectRuleQuerySlot))
                } returns RuleResponse(expectedDocumentRechten)
                val enkelvoudigInformatieobjectRechten = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject = enkelvoudigInformatieobject,
                    lock = null,
                    zaak = zaak
                )

                every {
                    opaEvaluationClient.readDocumentRechten(capture(documentZoekObjectRuleQuerySlot))
                } returns RuleResponse(expectedDocumentRechten)
                val documentZoekObjectRechten = policyService.readDocumentRechten(documentZoekObject)

                then("both paths send OPA the same decision-relevant DocumentData and return the same rechten") {
                    documentZoekObjectRechten shouldBe enkelvoudigInformatieobjectRechten

                    val documentData = enkelvoudigInformatieobjectRuleQuerySlot.captured.input.documentData
                    val documentZoekObjectData = documentZoekObjectRuleQuerySlot.captured.input.documentData
                    documentZoekObjectData.zaaktype shouldBe documentData.zaaktype
                    documentZoekObjectData.zaakOpen shouldBe documentData.zaakOpen
                    documentZoekObjectData.definitief shouldBe documentData.definitief
                    documentZoekObjectData.vergrendeld shouldBe documentData.vergrendeld
                    documentZoekObjectData.vergrendeldDoor shouldBe documentData.vergrendeldDoor
                    documentZoekObjectData.ondertekend shouldBe documentData.ondertekend
                    documentZoekObjectData.zaakspecifiekGeautoriseerd shouldBe documentData.zaakspecifiekGeautoriseerd
                    documentZoekObjectData.zaakspecifiekGeautoriseerd shouldBe false
                }
            }
        }

        given(
            "a document linked to a zaak that is zaakspecifiek geautoriseerd, read as an " +
                "EnkelvoudigInformatieObject and as a DocumentZoekObject"
        ) {
            val zaaktypeOmschrijving = "fakeZaaktype1"
            val zaak = createZaak()
            val zaakType = createZaakType(omschrijving = zaaktypeOmschrijving)
            val enkelvoudigInformatieobject = createEnkelvoudigInformatieObject()
            val documentZoekObject = createDocumentZoekObject(
                zaaktypeOmschrijving = zaaktypeOmschrijving,
                isZaakspecifiekGeautoriseerd = true
            )
            val expectedDocumentRechten = createDocumentRechten()
            val enkelvoudigInformatieobjectRuleQuerySlot = slot<RuleQuery<DocumentInput>>()
            val documentZoekObjectRuleQuerySlot = slot<RuleQuery<DocumentInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = "ZAAK_GEAUTORISEERD", waarde = "true")
            )
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("documentrechten are read for the document and for the equivalent document zoek object") {
                every {
                    opaEvaluationClient.readDocumentRechten(capture(enkelvoudigInformatieobjectRuleQuerySlot))
                } returns RuleResponse(expectedDocumentRechten)
                val enkelvoudigInformatieobjectRechten = policyService.readDocumentRechten(
                    enkelvoudigInformatieobject = enkelvoudigInformatieobject,
                    lock = null,
                    zaak = zaak
                )

                every {
                    opaEvaluationClient.readDocumentRechten(capture(documentZoekObjectRuleQuerySlot))
                } returns RuleResponse(expectedDocumentRechten)
                val documentZoekObjectRechten = policyService.readDocumentRechten(documentZoekObject)

                then("both paths send OPA zaakspecifiekGeautoriseerd = true and return the same rechten") {
                    documentZoekObjectRechten shouldBe enkelvoudigInformatieobjectRechten
                    enkelvoudigInformatieobjectRuleQuerySlot.captured.input.documentData.zaakspecifiekGeautoriseerd shouldBe true
                    documentZoekObjectRuleQuerySlot.captured.input.documentData.zaakspecifiekGeautoriseerd shouldBe true
                }
            }
        }
    }
})
