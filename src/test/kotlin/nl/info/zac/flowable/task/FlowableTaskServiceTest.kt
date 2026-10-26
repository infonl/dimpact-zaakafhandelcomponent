/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.flowable.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.zac.app.task.model.TaakSortering
import nl.info.zac.flowable.task.exception.TaskNotFoundException
import nl.info.zac.flowable.task.model.ValueChangeData
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.JSONB
import org.flowable.cmmn.api.CmmnTaskService
import org.flowable.common.engine.api.scope.ScopeTypes
import org.flowable.engine.HistoryService
import org.flowable.engine.TaskService
import org.flowable.identitylink.api.IdentityLink
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.Task
import org.flowable.task.api.TaskQuery
import org.flowable.task.api.history.HistoricTaskInstance

class FlowableTaskServiceTest : BehaviorSpec({
    val taskService = mockk<TaskService>()
    val cmmnTaskService = mockk<CmmnTaskService>()
    val historyService = mockk<HistoryService>()

    val flowableTaskService = FlowableTaskService(
        taskService,
        cmmnTaskService,
        historyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    afterEach {
        clearAllMocks()
    }

    given("An open task") {
        val taskId = "fakeTaskId"
        val task = mockk<Task>()

        every {
            taskService.createTaskQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns task

        `when`("the read open task method is called") {
            val returnedTask = flowableTaskService.readOpenTask(taskId)

            then("the open task is returned") {
                returnedTask shouldBe task
            }
        }
    }
    given("An closed task") {
        val taskId = "fakeTaskId"

        every {
            taskService.createTaskQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns null

        `when`("the read open task method is called") {
            val taskNotFoundException = shouldThrow<TaskNotFoundException> {
                flowableTaskService.readOpenTask(taskId)
            }

            then("the open task is returned") {
                taskNotFoundException.message shouldBe "No open task with id '$taskId' found"
            }
        }
    }
    given("A historic task") {
        val taskId = "fakeTaskId"
        val historicTaskInstance = mockk<HistoricTaskInstance>()

        every {
            historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns historicTaskInstance

        `when`("the read closed task method is called") {
            val returnedTask = flowableTaskService.readClosedTask(taskId)

            then("the historic task is returned") {
                returnedTask shouldBe historicTaskInstance
            }
        }
    }
    given("No historic task for the given task id") {
        val taskId = "fakeTaskId"

        every {
            historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns null

        `when`("the read open task method is called") {
            val taskNotFoundException = shouldThrow<TaskNotFoundException> {
                flowableTaskService.readClosedTask(taskId)
            }

            then("the open task is returned") {
                taskNotFoundException.message shouldBe "No historic task with id '$taskId' found"
            }
        }
    }
    given("No open task but a historic task for the given task id") {
        val taskId = "fakeTaskId"
        val historicTaskInstance = mockk<HistoricTaskInstance>()
        every {
            taskService.createTaskQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns null
        every {
            historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns historicTaskInstance

        `when`("the read task method is called") {
            val returnedTask = flowableTaskService.readTask(taskId)

            then("the historic task is returned") {
                returnedTask shouldBe historicTaskInstance
            }
        }
    }
    given("Open tasks") {
        val tasks = listOf(mockk<Task>())
        val taskQuery = mockk<TaskQuery>()
        every { taskService.createTaskQuery() } returns taskQuery
        every { taskQuery.orderByTaskName() } returns taskQuery
        every { taskQuery.asc() } returns taskQuery
        every { taskQuery.listPage(10, 20) } returns tasks

        `when`("the open tasks are listed sorted by task name in ascending order") {
            val returnedTasks = flowableTaskService.listOpenTasks(
                taakSortering = TaakSortering.TAAKNAAM,
                sorteerRichting = SorteerRichting.ASCENDING,
                firstResult = 10,
                maxResults = 20
            )

            then("the tasks are queried sorted by task name in ascending order") {
                returnedTasks shouldBe tasks
                verify(exactly = 1) {
                    taskQuery.orderByTaskName()
                    taskQuery.asc()
                }
            }
        }
    }
    given("An open CMMN task") {
        val taskId = "fakeTaskId"
        val task = mockk<Task> {
            every { id } returns taskId
            every { scopeType } returns ScopeTypes.CMMN
        }
        val historicTaskInstance = mockk<HistoricTaskInstance>()
        every { cmmnTaskService.complete(taskId) } just Runs
        every {
            historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns historicTaskInstance

        `when`("the task is completed") {
            val completedTask = flowableTaskService.completeTask(task)

            then("the task is completed using the CMMN task service") {
                completedTask shouldBe historicTaskInstance
                verify(exactly = 1) { cmmnTaskService.complete(taskId) }
                verify(exactly = 0) { taskService.complete(any()) }
            }
        }
    }
    given("An open BPMN task") {
        val taskId = "fakeTaskId"
        val task = mockk<Task> {
            every { id } returns taskId
            every { scopeType } returns null
        }
        val historicTaskInstance = mockk<HistoricTaskInstance>()
        every { taskService.complete(taskId) } just Runs
        every {
            historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns historicTaskInstance

        `when`("the task is completed") {
            val completedTask = flowableTaskService.completeTask(task)

            then("the task is completed using the BPMN task service") {
                completedTask shouldBe historicTaskInstance
                verify(exactly = 1) { taskService.complete(taskId) }
                verify(exactly = 0) { cmmnTaskService.complete(any()) }
            }
        }
    }
    given("An open task assigned to a candidate group") {
        val taskId = "fakeTaskId"
        val currentGroupId = "fakeCurrentGroupId"
        val newGroupId = "fakeNewGroupId"
        val explanation = "fakeExplanation"
        val identityLink = mockk<IdentityLink> {
            every { type } returns IdentityLinkType.CANDIDATE
            every { groupId } returns currentGroupId
        }
        val task = mockk<Task> {
            every { id } returns taskId
            every { identityLinks } returns listOf(identityLink)
        }
        val logEntryData = slot<String>()
        every { taskService.deleteGroupIdentityLink(taskId, currentGroupId, IdentityLinkType.CANDIDATE) } just Runs
        every { taskService.addGroupIdentityLink(taskId, newGroupId, IdentityLinkType.CANDIDATE) } just Runs
        every {
            historyService.createHistoricTaskLogEntryBuilder(task)
                .type(FlowableTaskService.USER_TASK_GROUP_CHANGED)
                .data(capture(logEntryData))
                .create()
        } just Runs
        every {
            taskService.createTaskQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns task

        `when`("the task is assigned to another group") {
            val returnedTask = flowableTaskService.assignTaskToGroup(task, newGroupId, explanation)

            then("the current candidate group is replaced and the change is logged in the task history") {
                returnedTask shouldBe task
                verify(exactly = 1) {
                    taskService.deleteGroupIdentityLink(taskId, currentGroupId, IdentityLinkType.CANDIDATE)
                    taskService.addGroupIdentityLink(taskId, newGroupId, IdentityLinkType.CANDIDATE)
                }
                JSONB.fromJson(logEntryData.captured, ValueChangeData::class.java) shouldBe
                    ValueChangeData(oldValue = currentGroupId, newValue = newGroupId, explanation = explanation)
            }
        }
    }
    given("An open task whose description is not changed") {
        val taskId = "fakeTaskId"
        val task = mockk<Task> {
            every { id } returns taskId
            every { description } returns "fakeDescription"
        }
        every { taskService.saveTask(task) } just Runs
        every {
            taskService.createTaskQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult()
        } returns task

        `when`("the task is updated") {
            val updatedTask = flowableTaskService.updateTask(task)

            then("the task is saved without a task history entry") {
                updatedTask shouldBe task
                verify(exactly = 1) { taskService.saveTask(task) }
                verify(exactly = 0) { historyService.createHistoricTaskLogEntryBuilder(any()) }
            }
        }
    }
})
