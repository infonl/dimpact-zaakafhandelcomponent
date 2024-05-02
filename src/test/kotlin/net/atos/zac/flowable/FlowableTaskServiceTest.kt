package net.atos.zac.flowable

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.exception.TaskNotFoundException
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
        val taskId = "dummyTaskId"
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
        val taskId = "dummyTaskId"

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
        val taskId = "dummyTaskId"
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
        val taskId = "dummyTaskId"

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
