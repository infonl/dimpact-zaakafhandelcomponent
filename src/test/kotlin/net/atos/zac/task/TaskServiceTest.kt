/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.task

import io.kotest.assertions.throwables.shouldThrow
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
import net.atos.zac.app.task.converter.RestTaskConverter
import net.atos.zac.app.task.model.createRestTaskAssignData
import net.atos.zac.app.task.model.createRestTaskDistributeData
import net.atos.zac.app.task.model.createRestTaskDistributeTask
import net.atos.zac.app.task.model.createRestTaskReleaseData
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexingService
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.Task

class TaskServiceTest : BehaviorSpec({
    val flowableTaskService = mockk<FlowableTaskService>()
    val indexingService = mockk<IndexingService>()
    val eventingService = mockk<EventingService>()
    val restTaskConverter = mockk<RestTaskConverter>()
    val loggedInUser = mockk<LoggedInUser>()
    val task1 = mockk<Task>()
    val task2 = mockk<Task>()
    val taskService = TaskService(
        flowableTaskService = flowableTaskService,
        indexingService = indexingService,
        eventingService = eventingService,
        restTaskConverter = restTaskConverter,
    )
    val taskId1 = "dummyTaskId1"
    val taskId2 = "dummyTaskId2"

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A task that has not yet been assigned to a specific group and user") {
        val restTaakToekennenGegevens = createRestTaskAssignData()
        val taskId = "dummyTaskId"
        val task = mockk<Task>()
        val identityLinks = mutableListOf<IdentityLinkInfo>()
        val updatedTaskAfterAssigningGroup = mockk<Task>()
        val updatedTaskAfterAssigningUser = mockk<Task>()
        val groupId = "dummyCurrentGroupId"
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task.assignee } returns null
        every { task.id } returns taskId
        every { task.identityLinks } returns identityLinks
        every { updatedTaskAfterAssigningUser.id } returns taskId
        every { restTaskConverter.extractGroupId(identityLinks) } returns groupId
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
        every { indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, true) } just runs

        When("the 'assign task' function is called with REST taak toekennen gegevens with a group and WITHOUT a user") {
            taskService.assignOrReleaseTask(
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
                    indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, true)
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

    Given("A task has been assigned to a user") {
        val restTaakToekennenGegevens = createRestTaskAssignData(behandelaarId = null)
        val taskId = "dummyTaskId"
        val task = mockk<Task>()
        val identityLinks = mutableListOf<IdentityLinkInfo>()
        val updatedTaskAfterAssigningUser = mockk<Task>()
        val groupId = "dummyCurrentGroupId"
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task.assignee } returns "dummyCurrentAssignee"
        every { task.id } returns taskId
        every { task.identityLinks } returns identityLinks
        every { updatedTaskAfterAssigningUser.id } returns taskId
        every { restTaskConverter.extractGroupId(identityLinks) } returns groupId
        every {
            flowableTaskService.releaseTask(
                task,
                restTaakToekennenGegevens.reden
            )
        } returns updatedTaskAfterAssigningUser
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, true) } just runs

        When("No user is set in the update") {
            taskService.assignOrReleaseTask(restTaakToekennenGegevens, task, loggedInUser)

            verify(exactly = 1) {
                flowableTaskService.releaseTask(task, restTaakToekennenGegevens.reden)
                indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, true)
            }
        }
    }

    Given("Two tasks that have not yet been assigned to a specific group and user") {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVerdelenGegevens = createRestTaskDistributeData(
            taken = restTaakVerdelenTaken
        )
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
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When("the 'assign tasks' function is called with REST taak verdelen gegevens") {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

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
                verify(exactly = 2) {
                    indexingService.indexeerDirect(
                        any<String>(),
                        ZoekObjectType.TAAK,
                        false
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
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVrijgevenGegevens = createRestTaskReleaseData(
            taken = restTaakVerdelenTaken
        )
        val updatedTaskAfterRelease1 = mockk<Task>()
        val updatedTaskAfterRelease2 = mockk<Task>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task1.id } returns taskId1
        every { task2.id } returns taskId2
        every { updatedTaskAfterRelease1.id } returns restTaakVerdelenTaken[0].taakId
        every { updatedTaskAfterRelease2.id } returns restTaakVerdelenTaken[1].taakId
        every { flowableTaskService.readOpenTask(taskId1) } returns task1
        every { flowableTaskService.readOpenTask(taskId2) } returns task2
        restTaakVrijgevenGegevens.let {
            every { flowableTaskService.releaseTask(task1, it.reden) } returns updatedTaskAfterRelease1
            every { flowableTaskService.releaseTask(task2, it.reden) } returns updatedTaskAfterRelease2
        }
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When(
            """"
                the 'release tasks' function is called with REST taak vrijgeven gegevens               
            """
        ) {
            taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser)

            Then(
                """taken are released, the index is updated and signalering and signaleringen and screen events are sent"""
            ) {
                verify(exactly = 2) {
                    flowableTaskService.releaseTask(any<Task>(), any())
                }
                verify(exactly = 2) {
                    indexingService.indexeerDirect(
                        any<String>(),
                        ZoekObjectType.TAAK,
                        false
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
    Given("Two open tasks that have not yet been assigned to a specific group and user") {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVerdelenGegevens = createRestTaskDistributeData(
            taken = restTaakVerdelenTaken,
            behandelaarGebruikersnaam = null
        )
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { task1.id } returns taskId1
        every { task2.id } returns taskId2
        every { task1.assignee } returns null
        every { task2.assignee } returns null
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } returns task2
        every {
            flowableTaskService.assignTaskToGroup(any(), any(), any())
        } returns task1 andThen task2
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When(
            """
            the 'assign tasks' function is called with REST taak verdelen gegevens without a assignee
            """
        ) {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            Then(
                """
                    the tasks are assigned to the group
                    """
            ) {
                verify(exactly = 2) {
                    flowableTaskService.assignTaskToGroup(any(), any(), any())
                }
                // since the tasks were not assigned to a user already, they should not be released
                // and no related screen events should be sent
                verify(exactly = 0) {
                    flowableTaskService.releaseTask(any<Task>(), any())
                }
            }
        }
    }
    Given(
        """
            Two tasks that have not yet been assigned to a specific group and user where the first
            task is a historical (closed) task and the second an open task 
            """
    ) {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVerdelenGegevens = createRestTaskDistributeData(
            taken = restTaakVerdelenTaken,
            behandelaarGebruikersnaam = null
        )
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { task2.id } returns taskId2
        every { task2.assignee } returns null
        every {
            flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId)
        } throws TaskNotFoundException("task not found!")
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } returns task2
        every {
            flowableTaskService.assignTaskToGroup(any(), any(), any())
        } returns task2
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When("the 'assign tasks' function is called with REST taak verdelen gegevens") {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            Then(
                """
                    the first task is skipped and the second task is assigned to the group,
                    """
            ) {
                verify(exactly = 1) {
                    flowableTaskService.assignTaskToGroup(any(), any(), any())
                }
                // since the tasks were not assigned to a user already, they should not be released
                // and no related screen events should be sent
                verify(exactly = 0) {
                    flowableTaskService.releaseTask(any<Task>(), any())
                }
            }
        }
    }
    Given("Two open tasks that are already assigned to a specific group and user") {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVerdelenGegevens = createRestTaskDistributeData(
            taken = restTaakVerdelenTaken,
            behandelaarGebruikersnaam = null
        )
        val screenEventSlot = mutableListOf<ScreenEvent>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val releasedTask1 = mockk<Task>()
        val releasedTask2 = mockk<Task>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task1.id } returns taskId1
        every { task2.id } returns taskId2
        every { releasedTask1.id } returns taskId1
        every { releasedTask2.id } returns taskId1
        every { task1.assignee } returns "dummyAssignee1"
        every { task2.assignee } returns "dummyAssignee2"
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } returns task2
        every {
            flowableTaskService.assignTaskToGroup(any(), any(), any())
        } returns task1 andThen task2
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { flowableTaskService.releaseTask(task1, restTaakVerdelenGegevens.reden) } returns releasedTask1
        every { flowableTaskService.releaseTask(task2, restTaakVerdelenGegevens.reden) } returns releasedTask2
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When(
            """
            the 'assign tasks' function is called with REST taak verdelen gegevens without a assignee
            """
        ) {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            Then(
                """
                    the tasks are assigned to the group and released from the current assignee
                    """
            ) {
                verify(exactly = 2) {
                    flowableTaskService.assignTaskToGroup(any(), any(), any())
                    // since the tasks were already assigned to a user already, they should also be released
                    // and related screen events should be sent
                    flowableTaskService.releaseTask(any<Task>(), any())
                    eventingService.send(any<SignaleringEvent<*>>())
                }
                // we expect four screen events, two for every task
                verify(exactly = 4) {
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }
    Given("Two open tasks that have not yet been assigned") {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVerdelenGegevens = createRestTaskDistributeData(
            taken = restTaakVerdelenTaken,
            behandelaarGebruikersnaam = null
        )
        val screenEventSlot = mutableListOf<ScreenEvent>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val releasedTask1 = mockk<Task>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task1.id } returns taskId1
        every { releasedTask1.id } returns taskId1
        every { task1.assignee } returns "dummyAssignee1"
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } throws RuntimeException("dummyError")
        every {
            flowableTaskService.assignTaskToGroup(any(), any(), any())
        } returns task1
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { flowableTaskService.releaseTask(task1, restTaakVerdelenGegevens.reden) } returns releasedTask1
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When(
            """
            when assigning the tasks a generic runtime exception is thrown when reading the second task 
            """
        ) {
            val exception = shouldThrow<RuntimeException> {
                taskService.assignTasks(
                    restTaakVerdelenGegevens,
                    loggedInUser
                )
            }

            Then(
                """
                    the search index is still updated and the TAKEN_VERDELEN screen event is still sent
                    """
            ) {
                exception.message shouldBe "dummyError"
                verify(exactly = 1) {
                    indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
                    flowableTaskService.assignTaskToGroup(any(), any(), any())
                }
                // we expect two screen events, one for the one succesfully assigned task
                // and the other for the TAKEN_VERDELEN screen event
                screenEventSlot.size shouldBe 2
                screenEventSlot[1].run {
                    objectType shouldBe ScreenEventType.ZAAK_TAKEN
                    opcode shouldBe Opcode.UPDATED
                }
            }
        }
    }
    Given(
        """
            Two tasks that have not yet been assigned to a specific group and user where the first
            task is a historical (closed) task and the second an open task
            """
    ) {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVrijgevenGegevens = createRestTaskReleaseData(
            taken = restTaakVerdelenTaken
        )
        val screenEventSlot = mutableListOf<ScreenEvent>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task2.id } returns taskId2
        every {
            flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId)
        } throws TaskNotFoundException("task not found!")
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } returns task2
        every {
            flowableTaskService.releaseTask(any(), any())
        } returns task2
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When("the 'release tasks' function is called with REST taak vrijgeven gegevens") {
            taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser)

            Then(
                """
                    the first task is skipped and the second task is released to the user,
                    """
            ) {
                verify(exactly = 1) {
                    flowableTaskService.releaseTask(any(), any())
                }
                // we expect twp screen events for the one succesfully released task
                screenEventSlot.size shouldBe 2
                screenEventSlot.map { it.objectType } shouldContainExactlyInAnyOrder listOf(
                    ScreenEventType.TAAK,
                    ScreenEventType.ZAAK_TAKEN
                )
                taakOpNaamSignaleringEventSlot.captured.run {
                    opcode shouldBe Opcode.UPDATED
                    actor shouldBe loggedInUser.id
                }
            }
        }
    }
    Given("Two open tasks") {
        val restTaakVerdelenTaken = listOf(
            createRestTaskDistributeTask(
                taakId = taskId1
            ),
            createRestTaskDistributeTask(
                taakId = taskId2
            )
        )
        val restTaakVerdelenGegevens = createRestTaskReleaseData(
            taken = restTaakVerdelenTaken,
        )
        val screenEventSlot = mutableListOf<ScreenEvent>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val releasedTask1 = mockk<Task>()

        every { loggedInUser.id } returns "dummyLoggedInUserId"
        every { task1.id } returns taskId1
        every { releasedTask1.id } returns taskId1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } throws RuntimeException("dummyError")
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { flowableTaskService.releaseTask(task1, restTaakVerdelenGegevens.reden) } returns releasedTask1
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        When(
            """
            when releasing the tasks a generic runtime exception is thrown when reading the second task 
            """
        ) {
            val exception = shouldThrow<RuntimeException> {
                taskService.releaseTasks(
                    restTaakVerdelenGegevens,
                    loggedInUser
                )
            }

            Then(
                """
                    the search index is still updated and the TAKEN_VERDELEN screen event is still sent
                    """
            ) {
                exception.message shouldBe "dummyError"
                verify(exactly = 1) {
                    indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
                    flowableTaskService.releaseTask(any(), any())
                }
                // we expect two screen events, one for the one succesfully released task
                // and the other for the TAKEN_VERDELEN screen event
                screenEventSlot.size shouldBe 2
                screenEventSlot[1].run {
                    objectType shouldBe ScreenEventType.ZAAK_TAKEN
                    opcode shouldBe Opcode.UPDATED
                }
            }
        }
    }
})
