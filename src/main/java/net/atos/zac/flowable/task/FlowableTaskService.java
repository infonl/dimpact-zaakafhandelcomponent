/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.task;

import static net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_UUID;
import static net.atos.zac.flowable.util.TaskUtil.isCmmnTask;
import static net.atos.zac.util.JsonbUtil.FIELD_VISIBILITY_STRATEGY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntry;

import net.atos.zac.app.task.model.TaakSortering;
import net.atos.zac.flowable.task.exception.TaskNotFoundException;
import net.atos.zac.flowable.task.model.ValueChangeData;
import nl.info.zac.shared.model.SorteerRichting;

@ApplicationScoped
@Transactional
public class FlowableTaskService {
    public static final String USER_TASK_DESCRIPTION_CHANGED = "USER_TASK_DESCRIPTION_CHANGED";
    public static final String USER_TASK_ASSIGNEE_CHANGED_CUSTOM = "USER_TASK_ASSIGNEE_CHANGED_CUSTOM";
    public static final String USER_TASK_GROUP_CHANGED = "USER_TASK_GROUP_CHANGED";

    private TaskService taskService;
    private CmmnTaskService cmmnTaskService;
    private HistoryService historyService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public FlowableTaskService() {
    }

    @Inject
    public FlowableTaskService(
            final TaskService taskService,
            final CmmnTaskService cmmnTaskService,
            final HistoryService historyService
    ) {
        this.taskService = taskService;
        this.cmmnTaskService = cmmnTaskService;
        this.historyService = historyService;
    }

    public List<Task> listOpenTasks(
            final TaakSortering taakSortering,
            final SorteerRichting sorteerRichting,
            final int firstResult,
            final int maxResults
    ) {
        final var taskQuery = taskService.createTaskQuery();
        if (taakSortering != null) {
            switch (taakSortering) {
                case ID -> taskQuery.orderByTaskId();
                case TAAKNAAM -> taskQuery.orderByTaskName();
                case CREATIEDATUM -> taskQuery.orderByTaskCreateTime();
                case FATALEDATUM -> taskQuery.orderByTaskDueDate();
                case BEHANDELAAR -> taskQuery.orderByTaskAssignee();
            }
            if (sorteerRichting.equals(SorteerRichting.ASCENDING)) {
                taskQuery.asc();
            } else {
                taskQuery.desc();
            }
        }
        return taskQuery.listPage(firstResult, maxResults);
    }

    public long countOpenTasks() {
        return taskService.createTaskQuery().count();
    }

    public List<Task> listOpenTasksDueNow() {
        return taskService.createTaskQuery()
                .taskAssigned()
                .taskDueBefore(tomorrow())
                .list();
    }

    public List<Task> listOpenTasksDueLater() {
        return taskService.createTaskQuery()
                .taskAssigned()
                .taskDueAfter(DateUtils.addSeconds(tomorrow(), -1))
                .list();
    }

    public List<? extends TaskInfo> listTasksForZaak(final UUID zaakUUID) {
        final List<TaskInfo> tasks = new ArrayList<>();
        tasks.addAll(taskService.createTaskQuery()
                .or()
                .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .endOr()
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .list());
        tasks.addAll(historyService.createHistoricTaskInstanceQuery()
                .or()
                .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .endOr()
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .finished()
                .list());
        return tasks;
    }

    public List<Task> listOpenTasksForZaak(final UUID zaakUUID) {
        return taskService.createTaskQuery()
                .or()
                .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .endOr()
                .list();
    }

    public long countOpenTasksForZaak(final UUID zaakUUID) {
        return taskService.createTaskQuery()
                .or()
                .caseVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .processVariableValueEquals(VAR_ZAAK_UUID, zaakUUID)
                .endOr()
                .count();
    }

    public List<HistoricTaskLogEntry> listHistorieForTask(final String taskId) {
        return historyService.createHistoricTaskLogEntryQuery().taskId(taskId).list();
    }

    public TaskInfo readTask(final String taskId) {
        final Task task = findOpenTask(taskId);
        if (task != null) {
            return task;
        }
        return readClosedTask(taskId);
    }

    public Task readOpenTask(final String taskId) {
        final Task task = findOpenTask(taskId);
        if (task == null) {
            throw new TaskNotFoundException(String.format("No open task with id '%s' found", taskId));
        }
        return task;
    }

    public HistoricTaskInstance readClosedTask(final String taskId) {
        final HistoricTaskInstance historicTaskInstance = findClosedTask(taskId);
        if (historicTaskInstance == null) {
            throw new TaskNotFoundException(String.format("No historic task with id '%s' found", taskId));
        }
        return historicTaskInstance;
    }

    public Task updateTask(final Task task) {
        final String oldDescription = readOpenTask(task.getId()).getDescription();
        taskService.saveTask(task);
        createHistoricTaskLogEntry(task, USER_TASK_DESCRIPTION_CHANGED, oldDescription, task.getDescription(), null);
        return readOpenTask(task.getId());
    }

    private void createHistoricTaskLogEntry(
            final Task task,
            final String type,
            final String oldValue,
            final String newValue,
            final String explanation
    ) {
        if (!StringUtils.equals(oldValue, newValue)) {
            historyService.createHistoricTaskLogEntryBuilder(task)
                    .type(type)
                    .data(FIELD_VISIBILITY_STRATEGY.toJson(
                            new ValueChangeData(
                                    Objects.toString(oldValue, StringUtils.EMPTY),
                                    Objects.toString(newValue, StringUtils.EMPTY),
                                    explanation
                            )
                    ))
                    .create();
        }
    }

    public HistoricTaskInstance completeTask(final Task task) {
        if (isCmmnTask(task)) {
            cmmnTaskService.complete(task.getId());
        } else {
            taskService.complete(task.getId());
        }
        return readClosedTask(task.getId());
    }

    public Task assignTaskToUser(final String taskId, final String userId, final String explanation) {
        final Task task = readOpenTask(taskId);
        taskService.setAssignee(taskId, userId);
        createHistoricTaskLogEntry(task, USER_TASK_ASSIGNEE_CHANGED_CUSTOM, task.getAssignee(), userId, explanation);
        return readOpenTask(taskId);
    }

    public Task assignTaskToGroup(final Task task, final String groupId, final String explanation) {
        final String currentGroupId = task.getIdentityLinks().stream()
                .filter(identityLinkInfo -> IdentityLinkType.CANDIDATE.equals(identityLinkInfo.getType()))
                .map(IdentityLinkInfo::getGroupId)
                .findFirst()
                .orElse(null);
        if (currentGroupId != null) {
            taskService.deleteGroupIdentityLink(task.getId(), currentGroupId, IdentityLinkType.CANDIDATE);
        }
        taskService.addGroupIdentityLink(task.getId(), groupId, IdentityLinkType.CANDIDATE);
        createHistoricTaskLogEntry(task, USER_TASK_GROUP_CHANGED, currentGroupId, groupId, explanation);
        return readOpenTask(task.getId());
    }

    public Task releaseTask(final Task task, final String explanation) {
        taskService.unclaim(task.getId());
        createHistoricTaskLogEntry(
                task,
                USER_TASK_ASSIGNEE_CHANGED_CUSTOM,
                task.getAssignee(),
                null,
                explanation
        );
        return readOpenTask(task.getId());
    }

    public Task findOpenTask(final String taskId) {
        return taskService.createTaskQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult();
    }

    private HistoricTaskInstance findClosedTask(final String taskId) {
        return historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .includeCaseVariables()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .singleResult();
    }

    private Date tomorrow() {
        return DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), 1);
    }
}
