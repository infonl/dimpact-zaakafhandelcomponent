/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.task.model.RestTaskDistributeData
import nl.info.zac.app.task.model.RestTaskReleaseData
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.flowable.task.readZaakUUID
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@AllOpen
class TaskService @Inject constructor(
    private val flowableTaskService: FlowableTaskService,
    private val indexingService: IndexingService,
    private val eventingService: EventingService,
    private val zrcClientService: ZrcClientService,
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService,
    private val taskHistoryService: TaskHistoryService
) {
    companion object {
        private val LOG = Logger.getLogger(TaskService::class.java.name)
    }

    /**
     * Assigns a single task to a group and/or user. This is the only place where the group and the assignee of an
     * existing task are written.
     *
     * @param groupId the group to assign the task to; when null, the group of the task is left as it is
     * @param userId the user to assign the task to; when null, the task is released from its assignee, if it has one
     * @return the task as returned by Flowable after the assignment to or release from a user, or the given task when
     * neither happened
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaak is zaakspecifiek
     * geautoriseerd but its zaaktype does not define the roltype; the task is then left unchanged
     */
    fun assignTask(task: Task, groupId: String?, userId: String?, reason: String?, loggedInUser: LoggedInUser): Task {
        val taskAssignedToOrReleasedFromUser = when {
            userId != null -> {
                grantZaakspecifiekeAutorisatieToNewAssignee(task, userId)
                flowableTaskService.assignTaskToUser(task.id, userId, reason)
            }
            task.assignee != null -> flowableTaskService.releaseTask(task, reason)
            else -> null
        }
        taskAssignedToOrReleasedFromUser?.let {
            eventingService.send(SignaleringEventUtil.event(SignaleringType.Type.TAAK_OP_NAAM, it, loggedInUser))
        }
        groupId?.let { flowableTaskService.assignTaskToGroup(task, it, reason) }
        val assignedTask = taskAssignedToOrReleasedFromUser ?: task
        eventingService.send(ScreenEventType.TAAK.updated(assignedTask))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(readZaakUUID(task)))
        indexingService.indexeerDirect(task.id, ZoekObjectType.TAAK, false)
        return assignedTask
    }

    /**
     * Assigns a list of tasks to a group and optionally also to a user, and updates the search index.
     * A task that cannot be found, or whose zaakspecifiek geautoriseerde zaak cannot authorise the user, is skipped.
     * This can be a long-running operation.
     */
    @WithSpan
    fun assignTasks(
        @SpanAttribute("restTaakVerdelenGegevens") restTaskDistributeData: RestTaskDistributeData,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null,
    ) {
        LOG.fine {
            "Started to assign ${restTaskDistributeData.taken.size} tasks " +
                "with screen event resource ID: '$screenEventResourceId'."
        }
        var numberOfAssignedTasks = 0
        try {
            numberOfAssignedTasks = restTaskDistributeData.taken.count {
                assignTaskFromBatch(taskId = it.taakId, restTaskDistributeData = restTaskDistributeData, loggedInUser = loggedInUser)
            }
        } finally {
            indexingService.commit()
            LOG.fine { "Successfully assigned $numberOfAssignedTasks tasks." }
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VERDELEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VERDELEN.updated(it))
            }
        }
    }

    /**
     * Returns any Flowable tasks that are part of a zaak for a given ZAAK UUID.
     */
    fun listTasksForZaak(zaakUUID: UUID): List<TaskInfo> = flowableTaskService.listTasksForZaak(zaakUUID)

    /**
     * Releases a list of tasks from their user and updates the search index. A task that cannot be found is skipped.
     * This can be a long-running operation.
     */
    @WithSpan
    fun releaseTasks(
        @SpanAttribute("restTaakVerdelenGegevens") restTaskReleaseData: RestTaskReleaseData,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null
    ) {
        LOG.fine {
            "Started to release ${restTaskReleaseData.taken.size} tasks " +
                "with screen event resource ID: '$screenEventResourceId'."
        }
        var numberOfReleasedTasks = 0
        try {
            numberOfReleasedTasks = restTaskReleaseData.taken.count {
                releaseTaskFromBatch(taskId = it.taakId, reason = restTaskReleaseData.reden, loggedInUser = loggedInUser)
            }
        } finally {
            indexingService.commit()
            LOG.fine { "Successfully released $numberOfReleasedTasks tasks." }
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VRIJGEVEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VRIJGEVEN.updated(it))
            }
        }
    }

    private fun assignTaskFromBatch(
        taskId: String,
        restTaskDistributeData: RestTaskDistributeData,
        loggedInUser: LoggedInUser
    ): Boolean =
        try {
            assignTask(
                task = flowableTaskService.readOpenTask(taskId),
                groupId = restTaskDistributeData.groepId,
                userId = restTaskDistributeData.behandelaarGebruikersnaam,
                reason = restTaskDistributeData.reden,
                loggedInUser = loggedInUser
            )
            true
        } catch (taskNotFoundException: TaskNotFoundException) {
            LOG.log(Level.SEVERE, taskNotFoundException) {
                "No open task with ID '$taskId' found while assigning tasks. Therefore it is skipped and not assigned."
            }
            false
        } catch (
            zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException: ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
        ) {
            LOG.log(Level.WARNING, zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException) {
                "Task with ID '$taskId' belongs to a zaakspecifiek geautoriseerde zaak whose zaaktype cannot authorise " +
                    "its taakbehandelaar. Therefore it is skipped and not assigned."
            }
            eventingService.send(ScreenEventType.TAAK.skipped(taskId))
            false
        }

    private fun releaseTaskFromBatch(taskId: String, reason: String?, loggedInUser: LoggedInUser): Boolean =
        try {
            assignTask(
                task = flowableTaskService.readOpenTask(taskId),
                groupId = null,
                userId = null,
                reason = reason,
                loggedInUser = loggedInUser
            )
            true
        } catch (taskNotFoundException: TaskNotFoundException) {
            LOG.log(Level.SEVERE, taskNotFoundException) {
                "No open task with ID '$taskId' found while releasing tasks. Therefore it is skipped and not released."
            }
            false
        }

    /**
     * Runs before the assignment is written, so that a missing roltype leaves the task unchanged and the new
     * assignee can open the task as soon as it is theirs.
     */
    private fun grantZaakspecifiekeAutorisatieToNewAssignee(task: Task, assignee: String) {
        if (task.assignee == assignee) return
        val zaak = zrcClientService.readZaak(readZaakUUID(task))
        if (zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, assignee)) {
            taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, assignee)
        }
    }
}
