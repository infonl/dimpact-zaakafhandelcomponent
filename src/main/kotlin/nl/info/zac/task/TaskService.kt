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
import nl.info.zac.app.task.model.RestTaskAssignData
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

    fun assignOrReleaseTask(
        restTaskAssignData: RestTaskAssignData,
        task: Task,
        loggedInUser: LoggedInUser
    ) {
        assignTask(
            task = task,
            zaakUuid = restTaskAssignData.zaakUuid,
            groupId = restTaskAssignData.groepId,
            userId = restTaskAssignData.behandelaarId,
            reason = restTaskAssignData.reden,
            loggedInUser = loggedInUser
        )
    }

    /**
     * Assigns a single task to a group and/or a user. This is the only place where the group and the assignee
     * of an existing task are written.
     *
     * @param groupId the group to assign; when null, the group of the task is left as it is
     * @param userId the user to assign; when null, the task is released from its current assignee, if it has one
     * @param releaseWithoutAssignee when true and no [userId] is given, the task is released even when it has no
     * assignee
     * @param performCommit whether the search index is committed right away; batch operations commit once at the end
     * @return the task as returned by the assignment to or release from a user, or the given task if neither happened
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaak is zaakspecifiek
     * geautoriseerd but its zaaktype does not define the roltype; the task is then left unchanged
     */
    @Suppress("LongParameterList")
    fun assignTask(
        task: Task,
        zaakUuid: UUID,
        groupId: String?,
        userId: String?,
        reason: String?,
        loggedInUser: LoggedInUser,
        releaseWithoutAssignee: Boolean = false,
        performCommit: Boolean = false
    ): Task {
        val updatedTask = when {
            userId != null -> assignTaskToUser(
                taskId = task.id,
                assignee = userId,
                loggedInUser = loggedInUser,
                explanation = reason
            )
            releaseWithoutAssignee || task.assignee != null -> releaseTask(
                task = task,
                loggedInUser = loggedInUser,
                reden = reason
            )
            else -> task
        }
        groupId?.let { flowableTaskService.assignTaskToGroup(task, it, reason) }
        sendScreenEventsOnTaskChange(updatedTask, zaakUuid)
        indexingService.indexeerDirect(task.id, ZoekObjectType.TAAK, performCommit)
        return updatedTask
    }

    /**
     * Assigns a list of tasks to a group and optionally also to an assignee,
     * sends corresponding screen events and updates the search index.
     * If no assignee was specified _and_ if the task is currently assigned to an assignee,
     * then the task will be released from the assignee.
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
        val succesfullyAssignedTaskIds = mutableListOf<String>()
        try {
            assignTasks(restTaskDistributeData, loggedInUser, succesfullyAssignedTaskIds)
        } finally {
            // always update the search index and send the screen event, also if exceptions were thrown
            indexingService.commit()
            LOG.fine { "Successfully assigned ${succesfullyAssignedTaskIds.size} tasks." }

            // if a screen event resource ID was specified, send a screen event
            // with the provided job ID so that it can be picked up by a client
            // that has created a websocket subscription to this event
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VERDELEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VERDELEN.updated(it))
            }
        }
    }

    /**
     * Assigns a task to a user and sends the 'taak op naam' signalering event.
     * When the task belongs to a zaakspecifiek geautoriseerde zaak, the user is granted access to that zaak
     * before the assignment is written, so that they can open the task as soon as it is theirs.
     *
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaak is zaakspecifiek
     * geautoriseerd but its zaaktype does not define the roltype; the task is then left unchanged
     */
    internal fun assignTaskToUser(
        taskId: String,
        assignee: String,
        loggedInUser: LoggedInUser,
        explanation: String?
    ): Task {
        grantZaakspecifiekeAutorisatieToNewAssignee(flowableTaskService.readOpenTask(taskId), assignee)
        return flowableTaskService.assignTaskToUser(taskId, assignee, explanation).also {
            eventingService.send(
                SignaleringEventUtil.event(
                    SignaleringType.Type.TAAK_OP_NAAM,
                    it,
                    loggedInUser
                )
            )
        }
    }

    /**
     * Returns any Flowable tasks that are part of a zaak for a given ZAAK UUID.
     */
    fun listTasksForZaak(zaakUUID: UUID): List<TaskInfo> = flowableTaskService.listTasksForZaak(zaakUUID)

    /**
     * Releases a list of tasks from any assigned user, sends corresponding screen events
     * and updates the search index.
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
        val taskIds = mutableListOf<String>()
        try {
            releaseTasks(restTaskReleaseData, loggedInUser, taskIds)
        } finally {
            indexingService.commit()
            LOG.fine { "Successfully released ${taskIds.size} tasks." }

            // if a screen event resource ID was specified, send a screen event
            // with the provided job ID so that it can be picked up by a client
            // that has created a websocket subscription to this event
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VRIJGEVEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VRIJGEVEN.updated(it))
            }
        }
    }

    private fun sendScreenEventsOnTaskChange(task: Task, zaakUuid: UUID) {
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(zaakUuid))
    }

    private fun assignTasks(
        restTaskDistributeData: RestTaskDistributeData,
        loggedInUser: LoggedInUser,
        successfullyAssignedTaskIds: MutableList<String>
    ) {
        restTaskDistributeData.taken.forEach { restTask ->
            try {
                assignTask(
                    task = flowableTaskService.readOpenTask(restTask.taakId),
                    zaakUuid = restTask.zaakUuid,
                    groupId = restTaskDistributeData.groepId,
                    userId = restTaskDistributeData.behandelaarGebruikersnaam,
                    reason = restTaskDistributeData.reden,
                    loggedInUser = loggedInUser
                )
                successfullyAssignedTaskIds.add(restTask.taakId)
            } catch (taskNotFoundException: TaskNotFoundException) {
                // continue assigning remaining tasks if a particular open task could not be found
                LOG.log(
                    Level.SEVERE,
                    "No open task with ID '${restTask.taakId}' found while assigning tasks. Skipping task.",
                    taskNotFoundException
                )
            } catch (
                zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException: ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
            ) {
                LOG.log(Level.WARNING, zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException) {
                    "Task with ID '${restTask.taakId}' belongs to a zaakspecifiek geautoriseerde zaak whose zaaktype " +
                        "cannot authorise its taakbehandelaar. Therefore it is skipped and not assigned."
                }
                eventingService.send(ScreenEventType.TAAK.skipped(restTask.taakId))
            }
        }
    }

    private fun releaseTasks(
        restTaskReleaseData: RestTaskReleaseData,
        loggedInUser: LoggedInUser,
        taskIds: MutableList<String>
    ) {
        restTaskReleaseData.taken.forEach {
            try {
                flowableTaskService.readOpenTask(it.taakId).let { task ->
                    assignTask(
                        task = task,
                        zaakUuid = it.zaakUuid,
                        groupId = null,
                        userId = null,
                        reason = restTaskReleaseData.reden,
                        loggedInUser = loggedInUser,
                        releaseWithoutAssignee = true
                    )
                    taskIds.add(task.id)
                }
            } catch (taskNotFoundException: TaskNotFoundException) {
                LOG.log(
                    Level.SEVERE,
                    "No open task with ID '${it.taakId}' found while releasing tasks. Skipping task.",
                    taskNotFoundException
                )
            }
        }
    }

    private fun grantZaakspecifiekeAutorisatieToNewAssignee(task: Task, assignee: String) {
        if (task.assignee == assignee) return
        val zaak = zrcClientService.readZaak(readZaakUUID(task))
        if (zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, assignee)) {
            taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, zaak, assignee)
        }
    }

    private fun releaseTask(
        task: Task,
        loggedInUser: LoggedInUser,
        reden: String?
    ) = flowableTaskService.releaseTask(task, reden).also {
        eventingService.send(
            SignaleringEventUtil.event(
                SignaleringType.Type.TAAK_OP_NAAM,
                it,
                loggedInUser
            )
        )
    }
}
