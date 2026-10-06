/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

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
import io.mockk.verifyOrder
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.app.task.model.createRestTaskAssignData
import nl.info.zac.app.task.model.createRestTaskDistributeData
import nl.info.zac.app.task.model.createRestTaskDistributeTask
import nl.info.zac.app.task.model.createRestTaskReleaseData
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import org.flowable.common.engine.api.scope.ScopeTypes
import org.flowable.task.api.Task
import java.util.UUID

@Suppress("LargeClass")
class TaskServiceTest : BehaviorSpec({
    val flowableTaskService = mockk<FlowableTaskService>()
    val indexingService = mockk<IndexingService>()
    val eventingService = mockk<EventingService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakspecifiekeAutorisatieService = mockk<ZaakspecifiekeAutorisatieService>()
    val taskHistoryService = mockk<TaskHistoryService>()
    val loggedInUser = mockk<LoggedInUser>()
    val task1 = mockk<Task>()
    val task2 = mockk<Task>()
    val taskService = TaskService(
        flowableTaskService = flowableTaskService,
        indexingService = indexingService,
        eventingService = eventingService,
        zrcClientService = zrcClientService,
        zaakspecifiekeAutorisatieService = zaakspecifiekeAutorisatieService,
        taskHistoryService = taskHistoryService
    )
    val taskId1 = "fakeTaskId1"
    val taskId2 = "fakeTaskId2"

    afterEach {
        checkUnnecessaryStub()
    }

    given("A task that has not yet been assigned to a specific group and user") {
        val taskId = "fakeTaskId"
        val restTaakToekennenGegevens = createRestTaskAssignData(taakId = taskId, behandelaarId = null)
        val task = mockk<Task>()
        val updatedTaskAfterAssigningGroup = mockk<Task>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { task.id } returns taskId
        every { task.assignee } returns null
        every {
            flowableTaskService.assignTaskToGroup(
                task,
                restTaakToekennenGegevens.groepId,
                restTaakToekennenGegevens.reden
            )
        } returns updatedTaskAfterAssigningGroup
        every { flowableTaskService.readOpenTask(taskId) } returns task
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, any()) } returns Unit

        `when`("the 'assign task' function is called with REST taak toekennen gegevens with a group and WITHOUT a user") {
            taskService.assignOrReleaseTask(
                restTaakToekennenGegevens,
                task,
                loggedInUser
            )

            then("the task is assigned to the group") {
                verify(exactly = 1) {
                    flowableTaskService.assignTaskToGroup(
                        task,
                        restTaakToekennenGegevens.groepId,
                        restTaakToekennenGegevens.reden
                    )
                }
            }

            and("the indexing service is called") {
                verify(exactly = 1) {
                    indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, false)
                }
            }

            and("the correct screen events should be created") {
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

    given("A task has already been assigned to a user") {
        val taskId = "fakeTaskId"
        val restTaakToekennenGegevens = createRestTaskAssignData(behandelaarId = null, taakId = taskId)
        val task = mockk<Task>()
        val updatedTaskAfterAssigningUser = mockk<Task>()
        val taakOpNaamSignaleringEventSlot = slot<SignaleringEvent<String>>()
        val screenEventSlot = mutableListOf<ScreenEvent>()

        every { loggedInUser.id } returns "fakeLoggedInUserId"
        every { task.assignee } returns "fakeCurrentAssignee"
        every { task.id } returns taskId
        every { updatedTaskAfterAssigningUser.id } returns taskId
        every {
            flowableTaskService.releaseTask(
                task,
                restTaakToekennenGegevens.reden
            )
        } returns updatedTaskAfterAssigningUser
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, any()) } returns Unit
        every { flowableTaskService.readOpenTask(taskId) } returns task
        every {
            flowableTaskService.assignTaskToGroup(
                task,
                restTaakToekennenGegevens.groepId,
                restTaakToekennenGegevens.reden
            )
        } returns task

        `when`("the 'assign task' function is called with REST taak toekennen gegevens with a group and WITHOUT a user") {
            taskService.assignOrReleaseTask(restTaakToekennenGegevens, task, loggedInUser)

            then("the task is released") {
                verify(exactly = 1) {
                    flowableTaskService.releaseTask(task, restTaakToekennenGegevens.reden)
                }
            }

            and("The indexing service is called") {
                verify(exactly = 1) {
                    indexingService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, false)
                }
            }
        }
    }

    given("Two tasks that have not yet been assigned to a specific group and user") {
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
        val zaak = createZaak()

        every { loggedInUser.id } returns "fakeLoggedInUserId"
        every { task1.id } returns taskId1
        every { task2.id } returns taskId2
        listOf(task1, task2).forEach {
            every { it.assignee } returns null
            every { it.scopeType } returns ScopeTypes.CMMN
            every { it.caseVariables } returns mapOf(VAR_ZAAK_UUID to zaak.uuid)
        }
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every {
            zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(
                zaak,
                "fakeBehandelaarGebruikersnaam"
            )
        } returns false
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

        `when`("the 'assign tasks' function is called with REST taak verdelen gegevens") {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            then(
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
    given("REST taak vrijgeven gegevens with two tasks") {
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

        every { loggedInUser.id } returns "fakeLoggedInUserId"
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

        `when`(
            """"
                the 'release tasks' function is called with REST taak vrijgeven gegevens               
            """
        ) {
            taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser)

            then(
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
    given("Two open tasks that have not yet been assigned to a specific group and user") {
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

        `when`(
            """
            the 'assign tasks' function is called with REST taak verdelen gegevens without a assignee
            """
        ) {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            then(
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
    given(
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

        `when`("the 'assign tasks' function is called with REST taak verdelen gegevens") {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            then(
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
    given("Two open tasks that are already assigned to a specific group and user") {
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

        every { loggedInUser.id } returns "fakeLoggedInUserId"
        every { task1.id } returns taskId1
        every { task2.id } returns taskId2
        every { releasedTask1.id } returns taskId1
        every { releasedTask2.id } returns taskId1
        every { task1.assignee } returns "fakeAssignee1"
        every { task2.assignee } returns "fakeAssignee2"
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

        `when`(
            """
            the 'assign tasks' function is called with REST taak verdelen gegevens without a assignee
            """
        ) {
            taskService.assignTasks(restTaakVerdelenGegevens, loggedInUser)

            then(
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
    given("Two open tasks that have not yet been assigned") {
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

        every { loggedInUser.id } returns "fakeLoggedInUserId"
        every { task1.id } returns taskId1
        every { releasedTask1.id } returns taskId1
        every { task1.assignee } returns "fakeAssignee1"
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } throws RuntimeException("fakeError")
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

        `when`(
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

            then(
                """
                    the search index is still updated and the TAKEN_VERDELEN screen event is still sent
                    """
            ) {
                exception.message shouldBe "fakeError"
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
    given(
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

        every { loggedInUser.id } returns "fakeLoggedInUserId"
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

        `when`("the 'release tasks' function is called with REST taak vrijgeven gegevens") {
            taskService.releaseTasks(restTaakVrijgevenGegevens, loggedInUser)

            then(
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
    given("Two open tasks") {
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

        every { loggedInUser.id } returns "fakeLoggedInUserId"
        every { task1.id } returns taskId1
        every { releasedTask1.id } returns taskId1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[0].taakId) } returns task1
        every { flowableTaskService.readOpenTask(restTaakVerdelenTaken[1].taakId) } throws RuntimeException("fakeError")
        every { eventingService.send(capture(screenEventSlot)) } just runs
        every { eventingService.send(capture(taakOpNaamSignaleringEventSlot)) } just runs
        every { flowableTaskService.releaseTask(task1, restTaakVerdelenGegevens.reden) } returns releasedTask1
        every {
            indexingService.indexeerDirect(any<String>(), ZoekObjectType.TAAK, false)
        } just runs
        every {
            indexingService.commit()
        } just runs

        `when`(
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

            then(
                """
                    the search index is still updated and the TAKEN_VERDELEN screen event is still sent
                    """
            ) {
                exception.message shouldBe "fakeError"
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
    context("Granting a new taakbehandelaar access to a zaakspecifiek geautoriseerde zaak") {
        given("a task assigned to a medewerker whose zaak gives the new assignee a rol") {
            val zaak = createZaak()
            val task = createTestTask(
                id = "fakeTaskId",
                assignee = "fakePreviousAssignee",
                caseVariables = mapOf(VAR_ZAAK_UUID to zaak.uuid)
            )
            every { loggedInUser.id } returns "fakeLoggedInUserId"
            every { flowableTaskService.readOpenTask("fakeTaskId") } returns task
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, "fakeNewAssignee")
            } returns true
            every {
                taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, zaak, "fakeNewAssignee")
            } just runs
            every { flowableTaskService.assignTaskToUser("fakeTaskId", "fakeNewAssignee", "fakeReason") } returns task
            every { eventingService.send(any<SignaleringEvent<*>>()) } just runs

            `when`("the task is assigned to a new medewerker") {
                taskService.assignTaskToUser(
                    taskId = "fakeTaskId",
                    assignee = "fakeNewAssignee",
                    loggedInUser = loggedInUser,
                    explanation = "fakeReason"
                )

                then("the new assignee is granted access and the grant is recorded before the task is assigned") {
                    verifyOrder {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(
                            zaak,
                            "fakeNewAssignee"
                        )
                        taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, zaak, "fakeNewAssignee")
                        flowableTaskService.assignTaskToUser("fakeTaskId", "fakeNewAssignee", "fakeReason")
                    }
                }
            }
        }

        given("a task whose zaak gives the new assignee no rol") {
            val zaak = createZaak()
            val task = createTestTask(id = "fakeTaskId", caseVariables = mapOf(VAR_ZAAK_UUID to zaak.uuid))
            every { loggedInUser.id } returns "fakeLoggedInUserId"
            every { flowableTaskService.readOpenTask("fakeTaskId") } returns task
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, "fakeNewAssignee")
            } returns false
            every { flowableTaskService.assignTaskToUser("fakeTaskId", "fakeNewAssignee", "fakeReason") } returns task
            every { eventingService.send(any<SignaleringEvent<*>>()) } just runs

            `when`("the task is assigned to the new assignee") {
                taskService.assignTaskToUser(
                    taskId = "fakeTaskId",
                    assignee = "fakeNewAssignee",
                    loggedInUser = loggedInUser,
                    explanation = "fakeReason"
                )

                then("the task is assigned, but nothing is recorded in its history") {
                    verify(exactly = 1) {
                        flowableTaskService.assignTaskToUser("fakeTaskId", "fakeNewAssignee", "fakeReason")
                    }
                    verify(exactly = 0) {
                        taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(any(), any(), any())
                    }
                }
            }
        }

        given("a task that is already assigned to the medewerker") {
            val task = createTestTask(id = "fakeTaskId", assignee = "fakeAssignee")
            every { loggedInUser.id } returns "fakeLoggedInUserId"
            every { flowableTaskService.readOpenTask("fakeTaskId") } returns task
            every { flowableTaskService.assignTaskToUser("fakeTaskId", "fakeAssignee", "fakeReason") } returns task
            every { eventingService.send(any<SignaleringEvent<*>>()) } just runs

            `when`("the task is assigned to that same medewerker again") {
                taskService.assignTaskToUser(
                    taskId = "fakeTaskId",
                    assignee = "fakeAssignee",
                    loggedInUser = loggedInUser,
                    explanation = "fakeReason"
                )

                then("the zaak is not read and no access is granted") {
                    verify(exactly = 0) {
                        zrcClientService.readZaak(any<UUID>())
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(any(), any())
                    }
                }
            }
        }

        given("two tasks to distribute, the first of a zaak whose zaaktype lacks the zaakspecifiek geautoriseerde medewerker roltype") {
            val zaakWithoutRoltype = createZaak()
            val zaak = createZaak()
            val taskOfZaakWithoutRoltype = createTestTask(
                id = "fakeTaskId1",
                caseVariables = mapOf(VAR_ZAAK_UUID to zaakWithoutRoltype.uuid)
            )
            val task = createTestTask(id = "fakeTaskId2", caseVariables = mapOf(VAR_ZAAK_UUID to zaak.uuid))
            val restTaskDistributeData = createRestTaskDistributeData(
                taken = listOf(
                    createRestTaskDistributeTask(taakId = "fakeTaskId1"),
                    createRestTaskDistributeTask(taakId = "fakeTaskId2")
                ),
                behandelaarGebruikersnaam = "fakeNewAssignee"
            )
            val screenEvents = mutableListOf<ScreenEvent>()
            every { loggedInUser.id } returns "fakeLoggedInUserId"
            every { flowableTaskService.readOpenTask("fakeTaskId1") } returns taskOfZaakWithoutRoltype
            every { flowableTaskService.readOpenTask("fakeTaskId2") } returns task
            every { zrcClientService.readZaak(zaakWithoutRoltype.uuid) } returns zaakWithoutRoltype
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(
                    zaakWithoutRoltype,
                    "fakeNewAssignee"
                )
            } throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException("fakeMessage")
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, "fakeNewAssignee")
            } returns false
            every { flowableTaskService.assignTaskToGroup(task, any(), any()) } returns task
            every { flowableTaskService.assignTaskToUser("fakeTaskId2", "fakeNewAssignee", any()) } returns task
            every { eventingService.send(capture(screenEvents)) } just runs
            every { eventingService.send(any<SignaleringEvent<*>>()) } just runs
            every { indexingService.indexeerDirect("fakeTaskId2", ZoekObjectType.TAAK, false) } just runs
            every { indexingService.commit() } just runs

            `when`("the tasks are distributed to a medewerker") {
                taskService.assignTasks(restTaskDistributeData, loggedInUser)

                then("the task of the zaak without the roltype is left completely unchanged") {
                    verify(exactly = 0) {
                        flowableTaskService.assignTaskToGroup(taskOfZaakWithoutRoltype, any(), any())
                        flowableTaskService.assignTaskToUser("fakeTaskId1", any(), any())
                    }
                }

                and("that task is reported as skipped") {
                    screenEvents.any { it.opcode == Opcode.SKIPPED && it.objectId.resource == "fakeTaskId1" } shouldBe true
                }

                and("the other task is still assigned") {
                    verify(exactly = 1) {
                        flowableTaskService.assignTaskToUser("fakeTaskId2", "fakeNewAssignee", any())
                    }
                }
            }
        }
    }
})
