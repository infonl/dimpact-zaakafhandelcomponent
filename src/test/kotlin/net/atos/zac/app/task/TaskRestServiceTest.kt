/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.task

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
import jakarta.servlet.http.HttpSession
import kotlinx.coroutines.test.runTest
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockRequest
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.zac.app.identity.model.createRESTUser
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.task.converter.RestTaskConverter
import net.atos.zac.app.task.converter.RestTaskHistoryConverter
import net.atos.zac.app.task.model.TaakStatus
import net.atos.zac.app.task.model.createRestTask
import net.atos.zac.app.task.model.createRestTaskAssignData
import net.atos.zac.app.task.model.createRestTaskDistributeData
import net.atos.zac.app.task.model.createRestTaskDistributeTask
import net.atos.zac.app.task.model.createRestTaskReleaseData
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_DOCUMENTEN_VERZENDEN_POST
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_VERZENDDATUM
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.TaakVariabelenService.TAAK_DATA_DOCUMENTEN_VERZENDEN_POST
import net.atos.zac.flowable.TaakVariabelenService.TAAK_DATA_VERZENDDATUM
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil.getTaakStatus
import net.atos.zac.formulieren.FormulierRuntimeService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createDocumentRechtenAllDeny
import net.atos.zac.policy.output.createTaakRechtenAllDeny
import net.atos.zac.policy.output.createWerklijstRechten
import net.atos.zac.policy.output.createWerklijstRechtenAllDeny
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.task.TaskService
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.zoeken.IndexeerService
import org.flowable.task.api.Task
import org.flowable.task.api.history.HistoricTaskInstance
import org.flowable.task.api.history.createHistoricTaskInstanceEntityImpl
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class TaskRestServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val eventingService = mockk<EventingService>()
    val httpSessionInstance = mockk<Instance<HttpSession>>()
    val indexeerService = mockk<IndexeerService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val restTaskConverter = mockk<RestTaskConverter>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zrcClientService = mockk<ZrcClientService>()
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val restInformatieobjectConverter = mockk<RESTInformatieobjectConverter>()
    val signaleringService = mockk<SignaleringService>()
    val taakHistorieConverter = mockk<RestTaskHistoryConverter>()
    val zgwApiService = mockk<ZGWApiService>()
    val taskService = mockk<TaskService>()
    val formulierRuntimeService = mockk<FormulierRuntimeService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()

    val taskRestService = TaskRestService(
        drcClientService = drcClientService,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        eventingService = eventingService,
        httpSession = httpSessionInstance,
        indexeerService = indexeerService,
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
        zaakVariabelenService = zaakVariabelenService
    )
    val loggedInUser = createLoggedInUser()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a task is not yet assigned") {
        val restTaakToekennenGegevens = createRestTaskAssignData()
        val task = mockk<Task>()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { flowableTaskService.readOpenTask(restTaakToekennenGegevens.taakId) } returns task
        every { getTaakStatus(task) } returns TaakStatus.NIET_TOEGEKEND
        every { task.assignee } returns ""
        every {
            taskService.assignTask(
                restTaakToekennenGegevens,
                task,
                loggedInUser
            )
        } just runs

        When("the task is assigned with a user who has permission") {
            every { policyService.readTaakRechten(task) } returns createTaakRechtenAllDeny(toekennen = true)

            taskRestService.assign(restTaakToekennenGegevens)

            Then(
                "the task is correctly assigned"
            ) {
                verify(exactly = 1) {
                    taskService.assignTask(
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
                taskRestService.assign(restTaakToekennenGegevens)
            }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }
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
        every { task.assignee } returns "dummyAssignee"
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
        } returns createEnkelvoudigInformatieObjectWithLockRequest()
        every { taakVariabelenService.setTaskData(task, restTaak.taakdata) } just runs
        every { taakVariabelenService.setTaskinformation(task, null) } just runs
        every { flowableTaskService.completeTask(task) } returns historicTaskInstance
        every { indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
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
        val restTaakDataKey = "dummyKey"
        val restTaakDataValue = "dummyValue"
        val signatureUUID = UUID.randomUUID()
        val restTaakData: MutableMap<String, Any> = mutableMapOf(
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
        every { task.assignee } returns "dummyAssignee"
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
            every { indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false) } just runs
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
    Given("REST taak verdeelgegevens to assign two tasks asynchronously") {
        val screenEventResourceId = "dummyScreenEventResourceId"
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

            runTest {
                taskRestService.distributeFromList(restTaakVerdelenGegevens)
                testScheduler.advanceUntilIdle()
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
                taskRestService.distributeFromList(restTaakVerdelenGegevens)
            }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }
    Given("REST taak vrijgeven gegevens to release two tasks asynchronously") {
        val screenEventResourceId = "dummyScreenEventResourceId"
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
            runTest {
                taskRestService.releaseFromList(restTaakVrijgevenGegevens)
                testScheduler.advanceUntilIdle()
            }

            Then("the tasks are assigned to the group and user") {
                verify(exactly = 1) {
                    taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser, screenEventResourceId)
                }
            }
        }
    }
})
