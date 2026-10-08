/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.planitems

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.mail.model.createRestMailGegevens
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CmmnService
import nl.info.zac.util.time.convertToDate
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.BrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.planitems.converter.RestPlanItemConverter
import nl.info.zac.app.planitems.model.UserEventListenerActie
import nl.info.zac.app.planitems.model.createRestHumanTaskData
import nl.info.zac.app.planitems.model.createRestUserEventListenerData
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.createMailAdres
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mailtemplates.model.createMailTemplate
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.task.TaskHistoryService
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import nl.info.test.org.flowable.task.api.createTestTask
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LargeClass")
class PlanItemsRestServiceTest : BehaviorSpec({
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val cmmnService = mockk<CmmnService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val resultaattypeReferenceService = mockk<ResultaattypeReferenceService>()
    val planItemConverter = mockk<RestPlanItemConverter>()
    val zgwApiService = mockk<ZgwApiService>()
    val indexingService = mockk<IndexingService>()
    val mailService = mockk<MailService>()
    val configurationService = mockk<ConfigurationService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val policyService = mockk<PolicyService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val zaakspecifiekeAutorisatieService = mockk<ZaakspecifiekeAutorisatieService>()
    val taskHistoryService = mockk<TaskHistoryService>()

    val planItemsRESTService = PlanItemsRestService(
        zaakVariabelenService,
        cmmnService,
        zrcClientService,
        zaaktypeConfigurationService,
        resultaattypeReferenceService,
        planItemConverter,
        zgwApiService,
        indexingService,
        mailService,
        configurationService,
        mailTemplateService,
        policyService,
        suspensionZaakHelper,
        loggedInUserInstance,
        zaakspecifiekeAutorisatieService,
        taskHistoryService
    )

    val planItemInstanceId = "fakePlanItemInstanceId"
    val planItemInstance = mockk<PlanItemInstance>()
    val zaakTypeUUID = UUID.randomUUID()
    val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
        zaaktypeUUID = zaakTypeUUID
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("doHumanTaskplanItem") {

        given("Valid REST human task data without a fatal date") {
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf("fakeKey" to "fakeValue"),
                fataledatum = null
            )
            val taskDataSlot = slot<Map<String, String>>()
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val loggedInUser = createLoggedInUser()
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { zaaktypeConfigurationService.findConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every { indexingService.addOrUpdateZaakOrThrow(zaak.uuid, false) } just runs
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId = planItemInstanceId,
                    groupId = restHumanTaskData.groep.id,
                    assignee = null,
                    dueDate = any(),
                    description = restHumanTaskData.toelichting,
                    taakdata = capture(taskDataSlot),
                    zaakUUID = zaak.uuid
                )
            } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A human task plan item is started from user that has access") {
                every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)

                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                then("A CMMN human task plan item is started and the zaak is re-indexed") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        indexingService.addOrUpdateZaakOrThrow(any(), any())
                    }
                }
                with(taskDataSlot.captured) {
                    get("fakeKey") shouldBe "fakeValue"
                }
            }

            `when`("the enkelvoudig informatieobject is updated by a user that has no access") {
                every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

                val exception = shouldThrow<PolicyException> {
                    planItemsRESTService.doHumanTaskplanItem(
                        restHumanTaskData
                    )
                }

                then("it throws exception with no message") { exception.message shouldBe null }
            }
        }

        given("REST human task data with a selected medewerker who gets a zaakspecifiek geautoriseerde medewerker rol") {
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                medewerker = createRestUser(id = "fakeTaakbehandelaarId"),
                taakdata = mapOf("fakeKey" to "fakeValue")
            )
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val loggedInUser = createLoggedInUser()
            val task = createTestTask(id = "fakeTaskId", assignee = "fakeTaakbehandelaarId")
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, "fakeTaakbehandelaarId")
            } returns true
            every { zaaktypeConfigurationService.findConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId = planItemInstanceId,
                    groupId = restHumanTaskData.groep.id,
                    assignee = "fakeTaakbehandelaarId",
                    dueDate = any(),
                    description = any(),
                    taakdata = any(),
                    zaakUUID = zaak.uuid
                )
            } just runs
            every { cmmnService.readOpenTaskForPlanItem(planItemInstanceId) } returns task
            every {
                taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, zaak, "fakeTaakbehandelaarId")
            } just runs
            every { indexingService.addOrUpdateZaakOrThrow(zaak.uuid, false) } just runs

            `when`("the human task plan item is started") {
                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                then("the medewerker is granted access before the taak is created, and the grant is recorded on the new taak") {
                    verifyOrder {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(
                            zaak,
                            "fakeTaakbehandelaarId"
                        )
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(
                            task,
                            zaak,
                            "fakeTaakbehandelaarId"
                        )
                    }
                }
            }
        }

        given(
            """
            REST human task data with a selected medewerker on a zaak whose zaaktype lacks the zaakspecifiek
            geautoriseerde medewerker roltype
            """
        ) {
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                medewerker = createRestUser(id = "fakeTaakbehandelaarId"),
                taakdata = mapOf("fakeKey" to "fakeValue")
            )
            val zaak = createZaak()
            val loggedInUser = createLoggedInUser()
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, "fakeTaakbehandelaarId")
            } throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException("fakeMessage")

            `when`("the human task plan item is started") {
                val roltypeNotFoundException = shouldThrow<ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException> {
                    planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)
                }

                then("no taak is created and the zaak is neither suspended nor mailed about") {
                    roltypeNotFoundException.message shouldBe "fakeMessage"
                    verify(exactly = 0) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        suspensionZaakHelper.suspendZaak(any(), any(), any())
                        mailService.sendMail(any(), any())
                    }
                }
            }
        }

        given("Valid REST human task data with a fatal date and with zaak opschorten set to true") {
            val opgeschorteZaak = createZaak()
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf(
                    "fakeKey" to "fakeValue",
                    "zaakOpschorten" to "true"
                ),
                fataledatum = LocalDate.now().plusDays(1)
            )
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val loggedInUser = createLoggedInUser()
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { zaaktypeConfigurationService.findConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every { indexingService.addOrUpdateZaakOrThrow(zaak.uuid, false) } just runs
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId = planItemInstanceId,
                    groupId = restHumanTaskData.groep.id,
                    assignee = null,
                    dueDate = restHumanTaskData.fataledatum?.let(::convertToDate),
                    description = restHumanTaskData.toelichting,
                    taakdata = any(),
                    zaakUUID = zaak.uuid
                )
            } just runs
            every {
                suspensionZaakHelper.suspendZaak(zaak, 1, "Aanvullende informatie opgevraagd")
            } returns opgeschorteZaak
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A human task plan item is started from user with access") {
                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                then("A CMMN human task plan item is started and the zaak is opgeschort and re-indexed") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        indexingService.addOrUpdateZaakOrThrow(any(), any())
                        suspensionZaakHelper.suspendZaak(any(), any(), any())
                    }
                }
            }
        }

        given("REST human task data with a user-set fatal date that comes after the fatal date of the related zaak") {
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf(
                    "fakeKey" to "fakeValue"
                ),
                fataledatum = LocalDate.now().plusDays(3)
            )
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val loggedInUser = createLoggedInUser()
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { zaaktypeConfigurationService.findConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A human task plan item is started") {
                shouldThrow<InputValidationFailedException> {
                    planItemsRESTService.doHumanTaskplanItem(
                        restHumanTaskData
                    )
                }
                then("An exception is thrown and the human task item is not started and the zaak is not indexed") {
                    verify(exactly = 0) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        indexingService.addOrUpdateZaakOrThrow(any(), any())
                    }
                }
            }
        }

        given("REST human task data with a calculated fatal date after the fatal date of the related zaak") {
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf(
                    "fakeKey" to "fakeValue"
                )
            )
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID")
            )
            val zaaktypeCmmnConfigurationMock = mockk<ZaaktypeConfiguration>()
            val loggedInUser = createLoggedInUser()

            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every {
                zaaktypeConfigurationService.findConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfigurationMock
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every {
                zaaktypeCmmnConfigurationMock.cmmnExtension?.findHumanTaskParameter(planItemInstanceId)
            } returns
                createHumanTaskParameters().apply {
                    doorlooptijd = 10
                }
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId = planItemInstanceId,
                    groupId = restHumanTaskData.groep.id,
                    assignee = null,
                    dueDate = convertToDate(zaak.uiterlijkeEinddatumAfdoening),
                    description = restHumanTaskData.toelichting,
                    taakdata = any(),
                    zaakUUID = zaak.uuid
                )
            } just runs
            every { indexingService.addOrUpdateZaakOrThrow(zaak.uuid, false) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A human task plan item is started") {
                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                then("The task is created with the zaak fatal date") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        indexingService.addOrUpdateZaakOrThrow(any(), any())
                    }
                }
            }
        }

        given("Additional info human task with a fatal date after the fatal date of the related zaak") {
            val numberOfDays = 3L
            val additionalInfoPlanItemInstanceId = FormulierDefinitie.AANVULLENDE_INFORMATIE.toString()
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = additionalInfoPlanItemInstanceId,
                taakdata = mapOf(
                    "fakeKey" to "fakeValue"
                ),
                fataledatum = LocalDate.now().plusDays(numberOfDays)
            )
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now()
            )
            val extendedZaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(numberOfDays)
            )
            val zaaktypeCmmnConfigurationMock = mockk<ZaaktypeConfiguration>()
            val loggedInUser = createLoggedInUser()

            every { cmmnService.readOpenPlanItem(additionalInfoPlanItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every {
                zaaktypeConfigurationService.findConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfigurationMock
            every { planItemInstance.planItemDefinitionId } returns additionalInfoPlanItemInstanceId
            every {
                zaaktypeCmmnConfigurationMock.cmmnExtension?.findHumanTaskParameter(additionalInfoPlanItemInstanceId)
            } returns
                createHumanTaskParameters().apply {
                    doorlooptijd = 10
                }
            every {
                suspensionZaakHelper.extendZaakFatalDate(zaak, numberOfDays, "Aanvullende informatie opgevraagd")
            } returns extendedZaak
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId = additionalInfoPlanItemInstanceId,
                    groupId = restHumanTaskData.groep.id,
                    assignee = null,
                    dueDate = restHumanTaskData.fataledatum?.let(::convertToDate),
                    description = restHumanTaskData.toelichting,
                    taakdata = any(),
                    zaakUUID = zaak.uuid
                )
            } just runs
            every { indexingService.addOrUpdateZaakOrThrow(zaak.uuid, false) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A human task plan item is started") {
                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                then("The task is created with its own fatal date") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        indexingService.addOrUpdateZaakOrThrow(any(), any())
                    }
                }

                and("zaak extend fatal date was performed") {
                    verify(exactly = 1) {
                        suspensionZaakHelper.extendZaakFatalDate(any(), any(), any())
                    }
                }
            }
        }

        given("Task data with send mail information") {
            val restHumanTaskData = createRestHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf(
                    "taakStuurGegevens.sendMail" to "true",
                    "taakStuurGegevens.mail" to "TAAK_AANVULLENDE_INFORMATIE",
                    "emailadres" to "example@example.com",
                    "body" to "body"
                ),
                fataledatum = null
            )
            val taskDataSlot = slot<Map<String, String>>()
            val mailGegevensSlot = slot<MailGegevens>()
            val zaak = createZaak(
                zaaktypeUri = URI("https://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val loggedInUser = createLoggedInUser()
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { zaaktypeConfigurationService.findConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every { mailTemplateService.readDefaultMailTemplate(Mail.TAAK_AANVULLENDE_INFORMATIE) } returns createMailTemplate()
            every { configurationService.readGemeenteNaam() } returns "gemeenteNaam"
            every { mailService.getGemeenteMailAdres() } returns createMailAdres()
            every { mailService.sendMail(capture(mailGegevensSlot), any()) } returns "body"
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId = planItemInstanceId,
                    groupId = restHumanTaskData.groep.id,
                    assignee = null,
                    dueDate = any(),
                    description = restHumanTaskData.toelichting,
                    taakdata = capture(taskDataSlot),
                    zaakUUID = zaak.uuid
                )
            } just runs
            every { indexingService.addOrUpdateZaakOrThrow(zaak.uuid, false) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A human task plan item is started from user that has access") {
                every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)

                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                then("A CMMN human task plan item is started and the zaak is re-indexed") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId = any(),
                            groupId = any(),
                            assignee = any(),
                            dueDate = any(),
                            description = any(),
                            taakdata = any(),
                            zaakUUID = any()
                        )
                        indexingService.addOrUpdateZaakOrThrow(any(), any())
                    }
                }

                and("the task data is set correctly") {
                    taskDataSlot.captured shouldBe restHumanTaskData.taakdata
                }

                and("email was sent for the task") {
                    verify(exactly = 1) {
                        mailService.sendMail(any(), any())
                    }
                    mailGegevensSlot.captured.vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
                }
            }

            `when`("the enkelvoudig informatieobject is updated by a user that has no access") {
                every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()
                val exception = shouldThrow<PolicyException> {
                    planItemsRESTService.doHumanTaskplanItem(
                        restHumanTaskData
                    )
                }
                then("it throws exception with no message") { exception.message shouldBe null }
            }
        }
    }

    context("doUserEventListenerPlanItem") {
        given("Zaak exists") {
            val zaak = createZaak(
                resultaat = URI("https://example.com/resultaat/${UUID.randomUUID()}"),
            )
            val mailGegevensSlot = slot<MailGegevens>()
            val restMailGegevens = createRestMailGegevens(
                vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.GEHEIM
            )
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                restMailGegevens = restMailGegevens,
                resultaattypeUuid = UUID.randomUUID(),
            )
            restUserEventListenerData.planItemInstanceId = planItemInstanceId
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                startenTaak = true,
                versturenEmail = true
            )
            every { cmmnService.startUserEventListenerPlanItem(any()) } just runs
            every { zgwApiService.closeZaak(zaak, restUserEventListenerData.resultaattypeUuid!!, null) } just runs
            every { configurationService.readGemeenteNaam() } returns "gemeenteNaam"
            every { mailService.sendMail(capture(mailGegevensSlot), any()) } returns ""
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("A user event to settle the zaak and send a corresponding email is planned") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("the zaak is settled and the email is sent") {
                    verify(exactly = 1) {
                        mailService.sendMail(any(), any())
                    }
                }

                and("the supplied vertrouwelijkheidaanduiding is passed through unchanged") {
                    mailGegevensSlot.captured.vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.GEHEIM
                }
            }
        }

        given("Zaak exists with resultaattype that has afleidingswijze EIGENSCHAP") {
            val zaak = createZaak(
                resultaat = null,
            )
            val resultaattypeUuid = UUID.randomUUID()
            val datumkenmerk = "testDatumkenmerk"
            val brondatumEigenschap = "2023-12-01T00:00:00.000+01:00"
            val brondatumArchiefprocedure = BrondatumArchiefprocedure().apply {
                afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP
            }
            brondatumArchiefprocedure.datumkenmerk = datumkenmerk
            val loggedInUser = createLoggedInUser()

            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                restMailGegevens = null,
                resultaattypeUuid = resultaattypeUuid,
                brondatum = brondatumEigenschap
            )

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every {
                zgwApiService.closeZaak(
                    zaak = zaak,
                    resultaatTypeUUID = resultaattypeUuid,
                    description = null,
                    brondatum = ZonedDateTime.parse(brondatumEigenschap).toLocalDate()
                )
            } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("the user event listener plan item is processed") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("the zaak is closed") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(
                            zaak = zaak,
                            resultaatTypeUUID = resultaattypeUuid,
                            description = null,
                            brondatum = ZonedDateTime.parse(brondatumEigenschap).toLocalDate()
                        )
                    }
                }
            }
        }

        given("Zaak exists with an unparsable brondatumEigenschap") {
            val zaak = createZaak(resultaat = null)
            val resultaattypeUuid = UUID.randomUUID()
            val loggedInUser = createLoggedInUser()

            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                restMailGegevens = null,
                resultaattypeUuid = resultaattypeUuid,
                brondatum = "not-a-date"
            )

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("the user event listener plan item is processed") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)
                }

                then("an InputValidationFailedException is thrown and the zaak is not closed") {
                    inputValidationFailedException.errorCode shouldBe ErrorCode.ERROR_CODE_VALIDATION_GENERIC
                    verify(exactly = 0) {
                        zgwApiService.closeZaak(zaak = any(), resultaatTypeUUID = any(), description = any(), brondatum = any())
                    }
                }
            }
        }

        given("Zaak without resultaat, when the zaak is closed") {
            val zaak = createZaak(resultaat = null)
            val resultaattypeUuid = UUID.randomUUID()
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                resultaattypeUuid = resultaattypeUuid,
                restMailGegevens = null
            )
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { zgwApiService.closeZaak(zaak, resultaattypeUuid, null) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called to close the zaak") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("closeZaak should be called to close the zaak") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(zaak, resultaattypeUuid, null)
                    }
                }
            }
        }

        given("Zaak with resultaattypeUuid and toelichting when handling zaak afhandelen") {
            val zaak = createZaak(resultaat = null)
            val resultaattypeUuid = UUID.randomUUID()
            val resultaatToelichting = "Zaak is afgehandeld"
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                resultaattypeUuid = resultaattypeUuid,
                restMailGegevens = null
            ).apply {
                this.resultaatToelichting = resultaatToelichting
            }
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { zgwApiService.closeZaak(zaak, resultaattypeUuid, resultaatToelichting) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called to handle zaak afhandelen") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("closeZaak should be called with the correct resultaattypeUuid and toelichting") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(zaak, resultaattypeUuid, resultaatToelichting)
                    }
                }
            }
        }

        given("Zaak without resultaattypeUuid when handling zaak afhandelen") {
            val zaak = createZaak(resultaat = null)
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                restMailGegevens = null
            ).apply {
                this.resultaattypeUuid = null
            }
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called without resultaattypeUuid") {
                val exception = shouldThrow<InputValidationFailedException> {
                    planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)
                }

                then("an exception should be thrown") {
                    exception.message shouldBe "Resultaattype UUID moet gevuld zijn bij het afhandelen van een zaak."
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_VALIDATION_GENERIC
                }

                and("closeZaak should not be called") {
                    verify(exactly = 0) {
                        zgwApiService.closeZaak(any(), any(), any())
                    }
                }
            }
        }

        given("Zaak that is not ontvankelijk during intake afronden") {
            val zaak = createZaak(resultaat = null)
            val nietOntvankelijkResultaattypeUuid = UUID.randomUUID()
            val resultaatToelichting = "Zaak is niet ontvankelijk"
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.INTAKE_AFRONDEN,
                restMailGegevens = null
            ).apply {
                this.isZaakOntvankelijk = false
                this.resultaatToelichting = resultaatToelichting
                this.planItemInstanceId = planItemInstanceId
            }
            val intakeAfrondenZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaak.zaaktype.extractUuid()
            )
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every {
                zaaktypeConfigurationService.readConfiguration(zaak.zaaktype.extractUuid())
            } returns intakeAfrondenZaaktypeCmmnConfiguration
            every {
                resultaattypeReferenceService.findNietOntvankelijkResultaattype(intakeAfrondenZaaktypeCmmnConfiguration)
            } returns createResultaatType(url = URI("https://example.com/resultaattypen/$nietOntvankelijkResultaattypeUuid"))
            every { zaakVariabelenService.setOntvankelijk(planItemInstance, false) } just runs
            every {
                zgwApiService.closeZaak(zaak, nietOntvankelijkResultaattypeUuid, resultaatToelichting)
            } just runs
            every { cmmnService.startUserEventListenerPlanItem(planItemInstanceId) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called for intake afronden with zaak not ontvankelijk") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("closeZaak should be called with niet ontvankelijk resultaattype") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(zaak, nietOntvankelijkResultaattypeUuid, resultaatToelichting)
                    }
                }

                and("setOntvankelijk should be called") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setOntvankelijk(planItemInstance, false)
                    }
                }
            }
        }

        given("Zaak that is ontvankelijk during intake afronden") {
            val zaak = createZaak(resultaat = null)
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.INTAKE_AFRONDEN,
                restMailGegevens = null
            ).apply {
                this.isZaakOntvankelijk = true
                this.planItemInstanceId = planItemInstanceId
            }
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.setOntvankelijk(planItemInstance, true) } just runs
            every { cmmnService.startUserEventListenerPlanItem(planItemInstanceId) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called for intake afronden with zaak ontvankelijk") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("closeZaak should not be called") {
                    verify(exactly = 0) {
                        zgwApiService.closeZaak(any(), any(), any())
                    }
                }

                and("setOntvankelijk should be called") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setOntvankelijk(planItemInstance, true)
                    }
                }
            }
        }

        given("Zaak that is not ontvankelijk but no nietOntvankelijkResultaattype configured") {
            val zaak = createZaak(resultaat = null)
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.INTAKE_AFRONDEN,
                restMailGegevens = null
            ).apply {
                this.isZaakOntvankelijk = false
                this.planItemInstanceId = planItemInstanceId
            }
            val geenResultaattypeZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaak.zaaktype.extractUuid(),
                nietOntvankelijkResultaattypeOmschrijving = null
            )
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every {
                zaaktypeConfigurationService.readConfiguration(zaak.zaaktype.extractUuid())
            } returns geenResultaattypeZaaktypeCmmnConfiguration
            every {
                resultaattypeReferenceService.findNietOntvankelijkResultaattype(geenResultaattypeZaaktypeCmmnConfiguration)
            } returns null
            every { zaakVariabelenService.setOntvankelijk(planItemInstance, false) } just runs
            every { cmmnService.startUserEventListenerPlanItem(planItemInstanceId) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`(
                "doUserEventListenerPlanItem is called for intake afronden with zaak not ontvankelijk but no resultaattype"
            ) {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("closeZaak should not be called") {
                    verify(exactly = 0) {
                        zgwApiService.closeZaak(any(), any(), any())
                    }
                }
            }
        }

        given("Zaak that is not ontvankelijk and whose zaaktype has no configuration") {
            val zaak = createZaak(resultaat = null)
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.INTAKE_AFRONDEN,
                restMailGegevens = null
            ).apply {
                this.isZaakOntvankelijk = false
                this.planItemInstanceId = planItemInstanceId
            }
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every {
                zaaktypeConfigurationService.readConfiguration(zaak.zaaktype.extractUuid())
            } throws ZaaktypeConfigurationNotFoundException("fakeMessage")
            every { zaakVariabelenService.setOntvankelijk(planItemInstance, false) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called for intake afronden with zaak not ontvankelijk") {
                shouldThrow<ZaaktypeConfigurationNotFoundException> {
                    planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)
                }

                then("the zaak is not closed and the user event listener is not started, so the failure is visible") {
                    verify(exactly = 0) {
                        zgwApiService.closeZaak(any(), any(), any())
                        cmmnService.startUserEventListenerPlanItem(any())
                    }
                }
            }
        }

        given("Zaak with a valid brondatum when setting the brondatum") {
            val zaak = createZaak(resultaat = null)
            val brondatum = "2023-12-01T00:00:00.000+01:00"
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.BRONDATUM_ZETTEN,
                restMailGegevens = null,
                brondatum = brondatum
            )
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(brondatumZetten = true)
            every {
                zgwApiService.setBrondatum(zaak, ZonedDateTime.parse(brondatum).toLocalDate())
            } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called to set the brondatum") {
                planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

                then("processBrondatumProcedure should be called with the parsed brondatum") {
                    verify(exactly = 1) {
                        zgwApiService.setBrondatum(zaak, ZonedDateTime.parse(brondatum).toLocalDate())
                    }
                }
            }
        }

        given("Zaak without a brondatum when setting the brondatum") {
            val zaak = createZaak(resultaat = null)
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.BRONDATUM_ZETTEN,
                restMailGegevens = null
            )
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(brondatumZetten = true)
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called without a brondatum") {
                val exception = shouldThrow<InputValidationFailedException> {
                    planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)
                }

                then("an exception should be thrown") {
                    exception.message shouldBe "Brondatum moet gevuld zijn bij het zetten van de brondatum van een zaak."
                    exception.errorCode shouldBe ErrorCode.ERROR_CODE_VALIDATION_GENERIC
                }

                and("processBrondatumProcedure should not be called") {
                    verify(exactly = 0) {
                        zgwApiService.setBrondatum(any(), any())
                    }
                }
            }
        }

        given("Zaak with a valid brondatum when setting the brondatum by a user without the brondatumZetten right") {
            val zaak = createZaak(resultaat = null)
            val brondatum = "2023-12-01T00:00:00.000+01:00"
            val restUserEventListenerData = createRestUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.BRONDATUM_ZETTEN,
                restMailGegevens = null,
                brondatum = brondatum
            )
            val loggedInUser = createLoggedInUser()

            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(startenTaak = true)
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("doUserEventListenerPlanItem is called to set the brondatum") {
                val exception = shouldThrow<PolicyException> {
                    planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)
                }

                then("it throws exception with no message") { exception.message shouldBe null }

                and("setBrondatum should not be called") {
                    verify(exactly = 0) {
                        zgwApiService.setBrondatum(any(), any())
                    }
                }
            }
        }
    }
})
