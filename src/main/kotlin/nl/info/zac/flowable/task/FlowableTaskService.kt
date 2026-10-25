/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import nl.info.zac.app.task.model.TaakSortering
import nl.info.zac.flowable.task.exception.TaskNotFoundException
import nl.info.zac.flowable.task.model.ValueChangeData
import nl.info.zac.flowable.util.isCmmnTask
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.FIELD_VISIBILITY_STRATEGY
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.time.DateUtils
import org.flowable.cmmn.api.CmmnTaskService
import org.flowable.engine.HistoryService
import org.flowable.engine.TaskService
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import org.flowable.task.api.history.HistoricTaskInstance
import org.flowable.task.api.history.HistoricTaskLogEntry
import java.util.Calendar
import java.util.Date
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class FlowableTaskService @Inject constructor(
    private val taskService: TaskService,
    private val cmmnTaskService: CmmnTaskService,
    private val historyService: HistoryService
) {
    companion object {
        const val USER_TASK_DESCRIPTION_CHANGED = "USER_TASK_DESCRIPTION_CHANGED"
        const val USER_TASK_ASSIGNEE_CHANGED_CUSTOM = "USER_TASK_ASSIGNEE_CHANGED_CUSTOM"
        const val USER_TASK_GROUP_CHANGED = "USER_TASK_GROUP_CHANGED"
    }

    fun listOpenTasks(
        taakSortering: TaakSortering,
        sorteerRichting: SorteerRichting,
        firstResult: Int,
        maxResults: Int
    ): List<Task> {
        val taskQuery = taskService.createTaskQuery()
        when (taakSortering) {
            TaakSortering.ID -> taskQuery.orderByTaskId()
            TaakSortering.TAAKNAAM -> taskQuery.orderByTaskName()
            TaakSortering.CREATIEDATUM -> taskQuery.orderByTaskCreateTime()
            TaakSortering.FATALEDATUM -> taskQuery.orderByTaskDueDate()
            TaakSortering.BEHANDELAAR -> taskQuery.orderByTaskAssignee()
        }
        if (sorteerRichting == SorteerRichting.ASCENDING) taskQuery.asc() else taskQuery.desc()
        return taskQuery.listPage(firstResult, maxResults)
    }

    fun countOpenTasks(): Long = taskService.createTaskQuery().count()

    fun listOpenTasksDueNow(): List<Task> =
        taskService.createTaskQuery()
            .taskAssigned()
            .taskDueBefore(tomorrow())
            .list()

    fun listOpenTasksDueLater(): List<Task> =
        taskService.createTaskQuery()
            .taskAssigned()
            .taskDueAfter(DateUtils.addSeconds(tomorrow(), -1))
            .list()

    fun listTasksForZaak(zaakUUID: UUID): List<TaskInfo> =
        taskService.createTaskQuery()
            .or()
            .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .endOr()
            .includeCaseVariables()
            .includeProcessVariables()
            .includeTaskLocalVariables()
            .includeIdentityLinks()
            .list() +
            historyService.createHistoricTaskInstanceQuery()
                .or()
                .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .endOr()
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .finished()
                .list()

    fun listOpenTasksForZaak(zaakUUID: UUID): List<Task> =
        taskService.createTaskQuery()
            .or()
            .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .endOr()
            .list()

    fun countOpenTasksForZaak(zaakUUID: UUID): Long =
        taskService.createTaskQuery()
            .or()
            .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
            .endOr()
            .count()

    fun listHistorieForTask(taskId: String): List<HistoricTaskLogEntry> =
        historyService.createHistoricTaskLogEntryQuery().taskId(taskId).list()

    fun readTask(taskId: String): TaskInfo = findOpenTask(taskId) ?: readClosedTask(taskId)

    fun readOpenTask(taskId: String): Task =
        findOpenTask(taskId) ?: throw TaskNotFoundException("No open task with id '$taskId' found")

    fun readClosedTask(taskId: String): HistoricTaskInstance =
        findClosedTask(taskId) ?: throw TaskNotFoundException("No historic task with id '$taskId' found")

    fun updateTask(task: Task): Task {
        val oldDescription = readOpenTask(task.id).description
        taskService.saveTask(task)
        createHistoricTaskLogEntry(
            task = task,
            type = USER_TASK_DESCRIPTION_CHANGED,
            oldValue = oldDescription,
            newValue = task.description,
            explanation = null
        )
        return readOpenTask(task.id)
    }

    fun completeTask(task: Task): HistoricTaskInstance {
        if (task.isCmmnTask()) {
            cmmnTaskService.complete(task.id)
        } else {
            taskService.complete(task.id)
        }
        return readClosedTask(task.id)
    }

    fun assignTaskToUser(taskId: String, userId: String, explanation: String?): Task {
        val task = readOpenTask(taskId)
        taskService.setAssignee(taskId, userId)
        createHistoricTaskLogEntry(
            task = task,
            type = USER_TASK_ASSIGNEE_CHANGED_CUSTOM,
            oldValue = task.assignee,
            newValue = userId,
            explanation = explanation
        )
        return readOpenTask(taskId)
    }

    fun assignTaskToGroup(task: Task, groupId: String, explanation: String?): Task {
        val currentGroupId = task.identityLinks
            .firstOrNull { it.type == IdentityLinkType.CANDIDATE }
            ?.groupId
        currentGroupId?.let {
            taskService.deleteGroupIdentityLink(task.id, it, IdentityLinkType.CANDIDATE)
        }
        taskService.addGroupIdentityLink(task.id, groupId, IdentityLinkType.CANDIDATE)
        createHistoricTaskLogEntry(
            task = task,
            type = USER_TASK_GROUP_CHANGED,
            oldValue = currentGroupId,
            newValue = groupId,
            explanation = explanation
        )
        return readOpenTask(task.id)
    }

    fun releaseTask(task: Task, explanation: String?): Task {
        taskService.unclaim(task.id)
        createHistoricTaskLogEntry(
            task = task,
            type = USER_TASK_ASSIGNEE_CHANGED_CUSTOM,
            oldValue = task.assignee,
            newValue = null,
            explanation = explanation
        )
        return readOpenTask(task.id)
    }

    fun findOpenTask(taskId: String): Task? =
        taskService.createTaskQuery()
            .taskId(taskId)
            .includeCaseVariables()
            .includeProcessVariables()
            .includeTaskLocalVariables()
            .includeIdentityLinks()
            .singleResult()

    private fun findClosedTask(taskId: String): HistoricTaskInstance? =
        historyService.createHistoricTaskInstanceQuery()
            .taskId(taskId)
            .includeCaseVariables()
            .includeProcessVariables()
            .includeTaskLocalVariables()
            .includeIdentityLinks()
            .singleResult()

    private fun createHistoricTaskLogEntry(
        task: Task,
        type: String,
        oldValue: String?,
        newValue: String?,
        explanation: String?
    ) {
        if (oldValue != newValue) {
            historyService.createHistoricTaskLogEntryBuilder(task)
                .type(type)
                .data(
                    FIELD_VISIBILITY_STRATEGY.toJson(
                        ValueChangeData(
                            oldValue = oldValue.orEmpty(),
                            newValue = newValue.orEmpty(),
                            explanation = explanation
                        )
                    )
                )
                .create()
        }
    }

    private fun tomorrow(): Date = DateUtils.addDays(DateUtils.truncate(Date(), Calendar.DATE), 1)
}
