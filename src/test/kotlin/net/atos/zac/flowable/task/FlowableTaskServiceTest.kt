/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import org.flowable.cmmn.api.CmmnTaskService
import org.flowable.engine.HistoryService
import org.flowable.engine.TaskService
import org.flowable.task.api.Task
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

    Given("An open task") {
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

        When("the read open task method is called") {
            val returnedTask = flowableTaskService.readOpenTask(taskId)

            Then("the open task is returned") {
                returnedTask shouldBe task
            }
        }
    }
    Given("An closed task") {
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

        When("the read open task method is called") {
            val taskNotFoundException = shouldThrow<TaskNotFoundException> {
                flowableTaskService.readOpenTask(taskId)
            }

            Then("the open task is returned") {
                taskNotFoundException.message shouldBe "No open task with id '$taskId' found"
            }
        }
    }
    Given("A historic task") {
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

        When("the read closed task method is called") {
            val returnedTask = flowableTaskService.readClosedTask(taskId)

            Then("the historic task is returned") {
                returnedTask shouldBe historicTaskInstance
            }
        }
    }
    Given("No historic task for the given task id") {
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

        When("the read open task method is called") {
            val taskNotFoundException = shouldThrow<TaskNotFoundException> {
                flowableTaskService.readClosedTask(taskId)
            }

            Then("the open task is returned") {
                taskNotFoundException.message shouldBe "No historic task with id '$taskId' found"
            }
        }
    }
})
