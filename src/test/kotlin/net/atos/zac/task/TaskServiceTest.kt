package net.atos.zac.task

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Scope
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.model.createRESTTaakToekennenGegevens
import net.atos.zac.app.taken.model.createRESTTaakVerdelenGegevens
import net.atos.zac.app.taken.model.createRESTTaakVerdelenTaak
import net.atos.zac.app.taken.model.createRESTTaakVrijgevenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.Task

class TaskServiceTest : BehaviorSpec({
    val flowableTaskService = mockk<FlowableTaskService>()
    val indexeerService = mockk<IndexeerService>()
    val eventingService = mockk<EventingService>()
    val restTaakConverter = mockk<RESTTaakConverter>()
    val tracer = mockk<Tracer>()
    val span = mockk<Span>()
    val scope = mockk<Scope>()
    val taskService = TaskService(
        flowableTaskService = flowableTaskService,
        indexeerService = indexeerService,
        eventingService = eventingService,
        restTaakConverter = restTaakConverter,
        tracer = tracer
    )

    beforeEach {
        // only check for unnecessary stubs on our mocked services and not on all mocks
        // using checkUnnecessaryStub() because that currently breaks the test
        checkUnnecessaryStub(
            flowableTaskService,
            indexeerService,
            eventingService,
            restTaakConverter
        )
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("A task that has not yet been assigned to a specific group and user") {
        val restTaakToekennenGegevens = createRESTTaakToekennenGegevens()
        val taskId = "dummyTaskId"
        val task = mockk<Task>()
        val identityLinks = mutableListOf<IdentityLinkInfo>()
        val updatedTaskAfterAssigningGroup = mockk<Task>()
        val updatedTaskAfterAssigningUser = mockk<Task>()
        val loggedInUser = mockk<LoggedInUser>()
        val groupId = "dummyCurrentGroupId"
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task.assignee } returns "dummyCurrentAssignee"
        every { task.id } returns taskId
        every { task.identityLinks } returns identityLinks
        every { updatedTaskAfterAssigningUser.id } returns taskId
        every { restTaakConverter.extractGroupId(identityLinks) } returns groupId
        every {
            flowableTaskService.assignTaskToGroup(
                task,
                restTaakToekennenGegevens.groepId,
                restTaakToekennenGegevens.reden
            )
        } returns updatedTaskAfterAssigningGroup
        every {
            flowableTaskService.assignTaskToUser(
                taskId,
                restTaakToekennenGegevens.behandelaarId,
                restTaakToekennenGegevens.reden
            )
        } returns updatedTaskAfterAssigningUser
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK) } just runs

        When("the 'assign task' function is called with REST taak toekennen gegevens with a group and user") {
            taskService.assignTask(
                restTaakToekennenGegevens,
                task,
                loggedInUser
            )
            Then("the tasks are assigned to the group and user") {
                verify(exactly = 1) {
                    flowableTaskService.assignTaskToGroup(
                        task,
                        restTaakToekennenGegevens.groepId,
                        restTaakToekennenGegevens.reden
                    )
                    flowableTaskService.assignTaskToUser(any(), any(), any())
                    indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK)
                }
                screenEventSlot.size shouldBe 2
                screenEventSlot.map { it.objectType } shouldContainExactlyInAnyOrder listOf(
                    ScreenEventType.TAAK,
                    ScreenEventType.ZAAK_TAKEN
                )
                screenEventSlot.map { it.opcode } shouldContainOnly listOf(
                    Opcode.UPDATED
                )
            }
        }
    }
    Given("Two tasks that have not yet been assigned to a specific group and user") {
        clearAllMocks()
        val restTaakVerdelenTaken = listOf(
            createRESTTaakVerdelenTaak(),
            createRESTTaakVerdelenTaak()
        )
        val restTaakVerdelenGegevens = createRESTTaakVerdelenGegevens(
            taken = restTaakVerdelenTaken
        )
        val taskId1 = "dummyTaskId1"
        val taskId2 = "dummyTaskId2"
        val loggedInUser = mockk<LoggedInUser>()
        val task1 = mockk<Task>()
        val task2 = mockk<Task>()
        val updatedTask1AfterAssigningGroup = mockk<Task>()
        val updatedTask2AfterAssigningGroup = mockk<Task>()
        val updatedTask1AfterAssigningUser = mockk<Task>()
        val updatedTask2AfterAssigningUser = mockk<Task>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task1.id } returns taskId1
        every { task2.id } returns taskId2
        every { updatedTask1AfterAssigningUser.id } returns taskId1
        every { updatedTask2AfterAssigningUser.id } returns taskId2
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } returns task2
        every {
            flowableTaskService.assignTaskToGroup(any(), any(), any())
        } returns updatedTask1AfterAssigningGroup andThen updatedTask2AfterAssigningGroup
        every {
            flowableTaskService.assignTaskToUser(any(), any(), any())
        } returns updatedTask1AfterAssigningUser andThen updatedTask2AfterAssigningUser
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every {
            indexeerService.indexeerDirect(restTaakVerdelenTaken.map { it.taakId }.toList(), ZoekObjectType.TAAK)
        } just runs
        every { tracer.spanBuilder(any()).setNoParent().startSpan() } returns span
        every { span.makeCurrent() } returns scope
        every { span.end() } just Runs
        every { scope.close() } just Runs

        When("the 'assign tasks' function is called with REST taak verdelen gegevens") {
            taskService.assignTasksAsync(restTaakVerdelenGegevens, loggedInUser).join()

            Then(
                """
                    the tasks are assigned to the group and user, the index is updated and 
                    signalering and screen events are sent
                    """
            ) {
                verify(exactly = 2) {
                    flowableTaskService.assignTaskToGroup(any(), any(), any())
                    flowableTaskService.assignTaskToUser(any(), any(), any())
                }
                verify(exactly = 1) {
                    indexeerService.indexeerDirect(
                        restTaakVerdelenTaken.map { it.taakId }.toList(),
                        ZoekObjectType.TAAK
                    )
                }
                // we expect 4 screen events to be sent, 2 for each task
                screenEventSlot.size shouldBe 4
                screenEventSlot.map { it.objectType } shouldContainExactlyInAnyOrder listOf(
                    ScreenEventType.TAAK,
                    ScreenEventType.ZAAK_TAKEN,
                    ScreenEventType.TAAK,
                    ScreenEventType.ZAAK_TAKEN
                )
                screenEventSlot.map { it.opcode } shouldContainOnly listOf(
                    Opcode.UPDATED
                )
            }
        }
    }
    Given("REST taak vrijgeven gegevens with two tasks") {
        clearAllMocks()
        val restTaakVerdelenTaken = listOf(
            createRESTTaakVerdelenTaak(),
            createRESTTaakVerdelenTaak()
        )
        val restTaakVrijgevenGegevens = createRESTTaakVrijgevenGegevens(
            taken = restTaakVerdelenTaken
        )
        val loggedInUser = mockk<LoggedInUser>()
        val updatedTaskAfterRelease1 = mockk<Task>()
        val updatedTaskAfterRelease2 = mockk<Task>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { updatedTaskAfterRelease1.id } returns restTaakVerdelenTaken[0].taakId
        every { updatedTaskAfterRelease2.id } returns restTaakVerdelenTaken[1].taakId
        restTaakVrijgevenGegevens.let {
            every { flowableTaskService.releaseTask(restTaakVerdelenTaken[0].taakId, it.reden) } returns updatedTaskAfterRelease1
            every { flowableTaskService.releaseTask(restTaakVerdelenTaken[1].taakId, it.reden) } returns updatedTaskAfterRelease2
        }
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every {
            indexeerService.indexeerDirect(restTaakVerdelenTaken.map { it.taakId }.toList(), ZoekObjectType.TAAK)
        } just runs
        every { tracer.spanBuilder(any()).setNoParent().startSpan() } returns span
        every { span.makeCurrent() } returns scope
        every { span.end() } just Runs
        every { scope.close() } just Runs

        When(
            """"
                the 'release tasks' function is called with REST taak vrijgeven gegevens               
            """
        ) {
            taskService.releaseTasksAsync(restTaakVrijgevenGegevens, loggedInUser).join()

            Then(
                """taken are released, the index is updated and signalering and signaleringen and screen events are sent"""
            ) {
                verify(exactly = 2) {
                    flowableTaskService.releaseTask(any(), any())
                }
                verify(exactly = 1) {
                    indexeerService.indexeerDirect(
                        restTaakVerdelenTaken.map { it.taakId }.toList(),
                        ZoekObjectType.TAAK
                    )
                }
                // we expect 4 screen events to be sent, 2 for each task
                screenEventSlot.size shouldBe 4
                screenEventSlot.map { it.objectType } shouldContainExactlyInAnyOrder listOf(
                    ScreenEventType.TAAK,
                    ScreenEventType.ZAAK_TAKEN,
                    ScreenEventType.TAAK,
                    ScreenEventType.ZAAK_TAKEN
                )
                screenEventSlot.map { it.opcode } shouldContainOnly listOf(
                    Opcode.UPDATED
                )
            }
        }
    }
})
