/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.json.Json
import jakarta.servlet.http.HttpSession
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.createTestTask
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_DOCUMENTEN_VERZENDEN_POST
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_VERZENDDATUM
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import net.atos.zac.formulieren.FormulierRuntimeService
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.test.org.flowable.task.service.impl.persistence.entity.createHistoricTaskInstanceEntityImpl
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.app.model.createRESTUser
import nl.info.zac.app.task.converter.RestTaskConverter
import nl.info.zac.app.task.converter.RestTaskHistoryConverter
import nl.info.zac.app.task.model.TaakStatus
import nl.info.zac.app.task.model.createRestTask
import nl.info.zac.app.task.model.createRestTaskAssignData
import nl.info.zac.app.task.model.createRestTaskDistributeData
import nl.info.zac.app.task.model.createRestTaskDistributeTask
import nl.info.zac.app.task.model.createRestTaskReleaseData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.identity.model.getFullName
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createDocumentRechtenAllDeny
import nl.info.zac.policy.output.createTaakRechtenAllDeny
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.task.TaskService
import org.flowable.task.api.Task
import org.flowable.task.api.history.HistoricTaskInstance
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.io.reader

class TaskRestServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val eventingService = mockk<EventingService>()
    val httpSessionInstance = mockk<Instance<HttpSession>>()
    val indexingService = mockk<IndexingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val restTaskConverter = mockk<RestTaskConverter>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zrcClientService = mockk<ZrcClientService>()
    val opschortenZaakHelper = mockk<SuspensionZaakHelper>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val signaleringService = mockk<SignaleringService>()
    val taakHistorieConverter = mockk<RestTaskHistoryConverter>()
    val zgwApiService = mockk<ZGWApiService>()
    val taskService = mockk<TaskService>()
    val formulierRuntimeService = mockk<FormulierRuntimeService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val testDispatcher = StandardTestDispatcher()
    val taskRestService = TaskRestService(
        drcClientService = drcClientService,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        eventingService = eventingService,
        httpSession = httpSessionInstance,
        indexingService = indexingService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        taakVariabelenService = taakVariabelenService,
        restTaskConverter = restTaskConverter,
        flowableTaskService = flowableTaskService,
        zrcClientService = zrcClientService,
        opschortenZaakHelper = opschortenZaakHelper,
        restInformatieobjectConverter = restInformatieobjectConverter,
        signaleringService = signaleringService,
        taakHistorieConverter = taakHistorieConverter,
        zgwApiService = zgwApiService,
        taskService = taskService,
        formulierRuntimeService = formulierRuntimeService,
        zaakVariabelenService = zaakVariabelenService,
        dispatcher = testDispatcher
    )
    val loggedInUser = createLoggedInUser()

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Assigning a task") {
        Given("a task is not yet assigned") {
            val restTaakToekennenGegevens = createRestTaskAssignData()
            val task = mockk<Task>()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { flowableTaskService.readOpenTask(restTaakToekennenGegevens.taakId) } returns task
            every { getTaakStatus(task) } returns TaakStatus.NIET_TOEGEKEND
            every { task.assignee } returns ""
            every {
                taskService.assignOrReleaseTask(
                    restTaakToekennenGegevens,
                    task,
                    loggedInUser
                )
            } just runs

            When("the task is assigned with a user who has permission") {
                every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(toekennen = true)

                taskRestService.assignTask(restTaakToekennenGegevens)

                Then(
                    "the task is correctly assigned"
                ) {
                    verify(exactly = 1) {
                        taskService.assignOrReleaseTask(
                            restTaakToekennenGegevens,
                            task,
                            loggedInUser
                        )
                    }
                }
            }

            When("assign is called from user with no permission") {
                every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny()

                val exception = shouldThrow<PolicyException> {
                    taskRestService.assignTask(restTaakToekennenGegevens)
                }

                Then("it throws exception with no message") { exception.message shouldBe null }
            }
        }
    }

    Context("Completing a task") {
        Given("a task is assigned to the current user") {
            val task = mockk<Task>()
            val zaak = mockk<Zaak>()
            val httpSession = mockk<HttpSession>()
            val historicTaskInstance = mockk<HistoricTaskInstance>()
            val restUser = createRESTUser(
                id = loggedInUser.id,
                name = loggedInUser.getFullName()
            )
            val documentUUID = UUID.randomUUID()
            val dateTime = ZonedDateTime.now()
            val restTaak = createRestTask(
                behandelaar = restUser,
                taakData = mutableMapOf(
                    TAAK_DATA_DOCUMENTEN_VERZENDEN_POST to documentUUID.toString(),
                    TAAK_DATA_VERZENDDATUM to dateTime.toString()
                )
            )
            val restTaakConverted = createRestTask(
                behandelaar = restUser
            )
            val enkelvoudigInformatieObjectUUID = UUID.randomUUID()
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(uuid = enkelvoudigInformatieObjectUUID)

            every { loggedInUserInstance.get() } returns loggedInUser
            every { task.assignee } returns "fakeAssignee"
            every { task.description = restTaak.toelichting } just runs
            every { task.dueDate = any() } just runs
            every { flowableTaskService.readOpenTask(restTaak.id) } returns task
            every { flowableTaskService.updateTask(task) } returns task
            every { zrcClientService.readZaak(restTaak.zaakUuid) } returns zaak
            every { httpSessionInstance.get() } returns httpSession
            every { httpSession.getAttribute(any<String>()) } returns null
            every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns enkelvoudigInformatieObject
            every {
                enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                    enkelvoudigInformatieObjectUUID, dateTime.toLocalDate(), null
                )
            } just Runs
            every { taakVariabelenService.setTaskData(task, restTaak.taakdata) } just runs
            every { taakVariabelenService.setTaskinformation(task, null) } just runs
            every { flowableTaskService.completeTask(task) } returns historicTaskInstance
            every { indexingService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
            every { historicTaskInstance.id } returns restTaak.id
            every { restTaskConverter.convert(historicTaskInstance) } returns restTaakConverted
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("'complete' is called from user with access") {
                every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(wijzigen = true)

                val restTaakReturned = taskRestService.completeTask(restTaak)

                Then(
                    "the task is completed and the search index service is invoked"
                ) {
                    restTaakReturned shouldBe restTaakConverted
                    verify(exactly = 1) {
                        flowableTaskService.completeTask(task)
                    }
                }
            }

            When("'complete' is called from user with access") {
                every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny()

                val exception = shouldThrow<PolicyException> {
                    taskRestService.completeTask(restTaak)
                }

                Then("it throws exception with no message") { exception.message shouldBe null }
            }
        }

        Given("a task with signature task data is assigned to the current user with a document that is signed") {
            val task = mockk<Task>()
            val restUser = createRESTUser(
                id = loggedInUser.id,
                name = loggedInUser.getFullName()
            )
            val restTaakDataKey = "fakeKey"
            val restTaakDataValue = "fakeValue"
            val signatureUUID = UUID.randomUUID()
            val restTaakData = mutableMapOf<String, Any>(
                restTaakDataKey to restTaakDataValue,
                "ondertekenen" to signatureUUID.toString()
            )
            val restTaak = createRestTask(
                behandelaar = restUser,
                taakData = restTaakData,
            )
            val restTaakConverted = createRestTask(
                behandelaar = restUser
            )
            every { task.assignee } returns "fakeAssignee"
            every { flowableTaskService.readOpenTask(restTaak.id) } returns task
            every { flowableTaskService.updateTask(task) } returns task
            every { taakVariabelenService.setTaskData(task, restTaak.taakdata) } just runs
            every { taakVariabelenService.setTaskinformation(task, null) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("'updateTaakdata' is called with changed description and due date from user with access") {
                every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(wijzigen = true)

                restTaak.apply {
                    toelichting = "changed"
                    fataledatum = LocalDate.parse("2024-03-19")
                }

                every { task.description = restTaak.toelichting } just runs
                every { task.dueDate = DateTimeConverterUtil.convertToDate(restTaak.fataledatum) } just runs
                every { task.id } returns restTaak.id

                val restTaakReturned = taskRestService.updateTaskData(restTaak)

                Then("the changes are stored") {
                    restTaakReturned shouldBe restTaak
                    verify(exactly = 1) {
                        flowableTaskService.updateTask(task)
                    }
                }
            }

            When("'complete' is called") {
                val zaak = createZaak()
                val historicTaskInstance = createHistoricTaskInstanceEntityImpl()
                val httpSession = mockk<HttpSession>()
                val enkelvoudigInformatieObjectUUID = UUID.randomUUID()
                val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
                    url = URI("http://example.com/$enkelvoudigInformatieObjectUUID")
                )
                every { zrcClientService.readZaak(restTaak.zaakUuid) } returns zaak
                every { flowableTaskService.completeTask(task) } returns historicTaskInstance
                every { indexingService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
                every { restTaskConverter.convert(historicTaskInstance) } returns restTaakConverted
                every { httpSessionInstance.get() } returns httpSession
                // in this test we assume there was no document uploaded to the http session beforehand
                every { httpSession.getAttribute("_FILE__${restTaak.id}__$restTaakDataKey") } returns null
                every { httpSession.getAttribute("_FILE__${restTaak.id}__ondertekenen") } returns null
                every {
                    drcClientService.readEnkelvoudigInformatieobject(signatureUUID)
                } returns enkelvoudigInformatieObject
                every {
                    policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
                } returns createDocumentRechtenAllDeny(wijzigen = true, ondertekenen = true)
                every {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                        enkelvoudigInformatieObjectUUID
                    )
                } just runs
                every { loggedInUserInstance.get() } returns loggedInUser

                val restTaakReturned = taskRestService.completeTask(restTaak)

                Then(
                    "the document is signed, the task is completed and the search index service is invoked"
                ) {
                    restTaakReturned shouldBe restTaakConverted
                    verify(exactly = 1) {
                        enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                            enkelvoudigInformatieObjectUUID
                        )
                        flowableTaskService.completeTask(task)
                    }
                }
            }
        }
    }

    Context("Assigning tasks from a list") {
        Given("REST taak verdeelgegevens to assign two tasks asynchronously") {
            val screenEventResourceId = "fakeScreenEventResourceId"
            val restTaakVerdelenGegevens = createRestTaskDistributeData(
                taken = listOf(
                    createRestTaskDistributeTask(),
                    createRestTaskDistributeTask()
                ),
                screenEventResourceId = screenEventResourceId
            )
            every {
                taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser, screenEventResourceId)
            } just Runs
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the 'verdelen vanuit lijst' function is called from user with access") {
                every {
                    policyService.readWerklijstRechten()
                } returns createWerklijstRechtenAllDeny(zakenTakenVerdelen = true)

                runTest(testDispatcher) {
                    taskRestService.assignTasksFromList(restTaakVerdelenGegevens)
                }

                Then("the tasks are assigned to the group and user") {
                    verify(exactly = 1) {
                        taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser, screenEventResourceId)
                    }
                }
            }

            When("the 'verdelen vanuit lijst' function is called from user with no access") {
                every { policyService.readWerklijstRechten() } returns createWerklijstRechtenAllDeny()

                val exception = shouldThrow<PolicyException> {
                    taskRestService.assignTasksFromList(restTaakVerdelenGegevens)
                }

                Then("it throws exception with no message") { exception.message shouldBe null }
            }
        }
    }

    Context("Releasing tasks from a list") {
        Given("REST taak vrijgeven gegevens to release two tasks asynchronously") {
            val screenEventResourceId = "fakeScreenEventResourceId"
            val restTaakVrijgevenGegevens = createRestTaskReleaseData(
                taken = listOf(
                    createRestTaskDistributeTask(),
                    createRestTaskDistributeTask()
                ),
                screenEventResourceId = screenEventResourceId
            )
            val werklijstRechten = createWerklijstRechten()
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every {
                taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser, screenEventResourceId)
            } just Runs
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the 'verdelen vanuit lijst' function is called") {
                runTest(testDispatcher) {
                    taskRestService.releaseTaskFromList(restTaakVrijgevenGegevens)
                }

                Then("the tasks are assigned to the group and user") {
                    verify(exactly = 1) {
                        taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser, screenEventResourceId)
                    }
                }
            }
        }
    }

    Context("Listing tasks for a zaak") {
        Given("Two Flowable tasks for a zaak") {
            val zaak = createZaak()
            val tasks = listOf(
                createTestTask(id = "fakeId1"),
                createTestTask(id = "fakeId2")
            )
            val restTasks = listOf(
                createRestTask(id = "fakeId1"),
                createRestTask(id = "fakeId2")
            )
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak).lezen } returns true
            every { taskService.listTasksForZaak(zaak.uuid) } returns tasks
            every { restTaskConverter.convert(tasks) } returns restTasks

            When("the tasks are listed for this zaak") {
                val returnedRestTasks = taskRestService.listTasksForZaak(zaak.uuid)

                Then("the tasks are returned") {
                    returnedRestTasks shouldBe restTasks
                    verify(exactly = 1) {
                        taskService.listTasksForZaak(zaak.uuid)
                    }
                }
            }
        }
    }

    Context("Reading a task") {
        Given("A valid task ID with an open task and a REST task with a form.io form") {
            val taskId = "validTaskId"
            val zaakUuid = UUID.randomUUID()
            val taskInfo = createTestTask(
                id = taskId,
                caseVariables = mapOf(
                    "zaakUUID" to zaakUuid
                )
            )
            val restTask = createRestTask(
                id = taskId,
                zaakUuid = zaakUuid,
                formioFormulier = Json.createReader("{}".reader()).readObject()
            )
            every { loggedInUserInstance.get() } returns loggedInUser
            every { signaleringService.deleteSignaleringen(any()) } returns 2
            every { flowableTaskService.readTask(taskId) } returns taskInfo
            every { policyService.readTaakRechten(taskInfo).lezen } returns true
            every { restTaskConverter.convert(taskInfo) } returns restTask
            every { formulierRuntimeService.renderFormioFormulier(restTask) } returns restTask.formioFormulier
            every { zaakVariabelenService.readProcessZaakdata(zaakUuid) } returns mapOf(
                "fakeKey" to "fakeValue"
            )

            When("readTask is called") {
                val result = taskRestService.readTask(taskId)

                Then("signaleringen are deleted and the task is returned with rendered forms and added zaakdata") {
                    result shouldBe restTask
                    verify(exactly = 2) {
                        signaleringService.deleteSignaleringen(any())
                    }
                    verify(exactly = 1) {
                        formulierRuntimeService.renderFormioFormulier(restTask)
                    }
                }
            }
        }

        Given("An invalid task ID") {
            val taskId = "invalidTaskId"
            every { flowableTaskService.readTask(taskId) } throws TaskNotFoundException("Task not found")

            When("readTask is called") {
                val exception = shouldThrow<TaskNotFoundException> {
                    taskRestService.readTask(taskId)
                }

                Then("a TaskNotFoundException is thrown") {
                    exception.message shouldBe "Task not found"
                }
            }
        }
    }
})
