package net.atos.zac.task

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.model.createRESTTaakToekennenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.flowable.task.api.Task

class TaskServiceTest : BehaviorSpec({
    val flowableTaskService = mockk<FlowableTaskService>()
    val indexeerService = mockk<IndexeerService>()
    val eventingService = mockk<EventingService>()
    val restTaakConverter = mockk<RESTTaakConverter>()
    val taskService = TaskService(
        flowableTaskService = flowableTaskService,
        indexeerService = indexeerService,
        eventingService = eventingService,
        restTaakConverter = restTaakConverter
    )

    beforeEach {
        checkUnnecessaryStub(
            flowableTaskService,
            indexeerService,
            eventingService,
            restTaakConverter
        )
    }

    Given("A task that has not been assigned yet to a specific group and user") {
        val restTaakToekennenGegevens = createRESTTaakToekennenGegevens()
        val taskId = "dummyTaskId"
        val task = mockk<Task>()
        val updatedTaskAfterAssigningGroup = mockk<Task>()
        val updatedTaskAfterAssigningUser = mockk<Task>()
        val loggedInUser = mockk<LoggedInUser>()
        val groupId = "dummyCurrentGroupId"
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task.assignee } returns "dummyCurrentAssignee"
        every { task.id } returns taskId
        every { updatedTaskAfterAssigningGroup.id } returns taskId
        every { updatedTaskAfterAssigningUser.id } returns taskId
        every { restTaakConverter.extractGroupId(task.identityLinks) } returns groupId
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
                    flowableTaskService.assignTaskToGroup(any(), any(), any())
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
})
