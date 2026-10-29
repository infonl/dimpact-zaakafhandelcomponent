/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.event.EventingService
import nl.info.zac.flowable.task.FlowableTaskService
import nl.info.zac.flowable.task.exception.TaskNotFoundException
import nl.info.zac.flowable.task.readZaakUUID
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.event.signaleringEvent
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType
import nl.info.zac.task.model.TaskDistribution
import nl.info.zac.util.AllOpen
import nl.info.zac.websocket.event.ScreenEventType
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import nl.info.zac.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.withLock

@AllOpen
class TaskService @Inject constructor(
    private val flowableTaskService: FlowableTaskService,
    private val indexingService: IndexingService,
    private val eventingService: EventingService,
    private val zrcClientService: ZrcClientService,
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService,
    private val taskHistoryService: TaskHistoryService,
    private val signaleringService: SignaleringService
) {
    companion object {
        private val LOG = Logger.getLogger(TaskService::class.java.name)
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
        @SpanAttribute("taskDistribution") taskDistribution: TaskDistribution,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null,
    ) {
        LOG.fine {
            "Started to assign ${taskDistribution.taskIds.size} tasks " +
                "with screen event resource ID: '$screenEventResourceId'."
        }
        val succesfullyAssignedTaskIds = mutableListOf<String>()
        try {
            assignTasks(taskDistribution, loggedInUser, succesfullyAssignedTaskIds)
        } finally {
            indexingService.commit()
            LOG.fine { "Successfully assigned ${succesfullyAssignedTaskIds.size} tasks." }
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VERDELEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VERDELEN.updated(it))
            }
        }
    }

    fun assignTask(
        task: Task,
        groupId: String,
        username: String?,
        reason: String?,
        loggedInUser: LoggedInUser
    ): Task {
        val assignedTask = when {
            username != null -> zaakspecifiekeAutorisatieService.lockForZaak(readZaakUUID(task)).withLock {
                grantZaakspecifiekeAutorisatieToNewAssignee(task, username)
                flowableTaskService.assignTaskToUser(task.id, username, reason)
                    .also { deleteTaakOpNaamSignaleringenExceptFor(it, username) }
            }.also { sendTaakOpNaamSignalering(it, loggedInUser) }
            task.assignee != null -> releaseTask(task, reason, loggedInUser)
            else -> task
        }
        flowableTaskService.assignTaskToGroup(task, groupId, reason)
        sendScreenEventsAndIndex(task)
        return assignedTask
    }

    private fun releaseTask(task: Task, reason: String?, loggedInUser: LoggedInUser): Task =
        zaakspecifiekeAutorisatieService.lockForZaak(readZaakUUID(task)).withLock {
            flowableTaskService.releaseTask(task, reason).also { deleteTaakOpNaamSignaleringenExceptFor(it, null) }
        }.also { sendTaakOpNaamSignalering(it, loggedInUser) }

    fun listTasksForZaak(zaakUUID: UUID): List<TaskInfo> = flowableTaskService.listTasksForZaak(zaakUUID)

    @WithSpan
    fun releaseTasks(
        @SpanAttribute("taskIds") taskIds: List<String>,
        reason: String?,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null
    ) {
        LOG.fine {
            "Started to release ${taskIds.size} tasks " +
                "with screen event resource ID: '$screenEventResourceId'."
        }
        val releasedTaskIds = mutableListOf<String>()
        try {
            releaseTasks(taskIds, reason, loggedInUser, releasedTaskIds)
        } finally {
            indexingService.commit()
            LOG.fine { "Successfully released ${releasedTaskIds.size} tasks." }
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VRIJGEVEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VRIJGEVEN.updated(it))
            }
        }
    }

    private fun assignTasks(
        taskDistribution: TaskDistribution,
        loggedInUser: LoggedInUser,
        successfullyAssignedTaskIds: MutableList<String>
    ) {
        taskDistribution.taskIds.forEach { taskId ->
            try {
                assignTask(
                    task = flowableTaskService.readOpenTask(taskId),
                    groupId = taskDistribution.groupId,
                    username = taskDistribution.username,
                    reason = taskDistribution.reason,
                    loggedInUser = loggedInUser
                )
                successfullyAssignedTaskIds.add(taskId)
            } catch (taskNotFoundException: TaskNotFoundException) {
                LOG.log(
                    Level.SEVERE,
                    "No open task with ID '$taskId' found while assigning tasks. Skipping task.",
                    taskNotFoundException
                )
            } catch (
                zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException: ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
            ) {
                LOG.log(Level.WARNING, zaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException) {
                    "Task with ID '$taskId' belongs to a zaakspecifiek geautoriseerde zaak whose zaaktype " +
                        "cannot authorise its taakbehandelaar. Therefore it is skipped and not assigned."
                }
                eventingService.send(ScreenEventType.TAAK.skipped(taskId))
            }
        }
    }

    private fun releaseTasks(
        taskIds: List<String>,
        reason: String?,
        loggedInUser: LoggedInUser,
        releasedTaskIds: MutableList<String>
    ) {
        taskIds.forEach { taskId ->
            try {
                flowableTaskService.readOpenTask(taskId).let { task ->
                    releaseTask(task = task, reason = reason, loggedInUser = loggedInUser)
                    sendScreenEventsAndIndex(task)
                    releasedTaskIds.add(task.id)
                }
            } catch (taskNotFoundException: TaskNotFoundException) {
                LOG.log(
                    Level.SEVERE,
                    "No open task with ID '$taskId' found while releasing tasks. Skipping task.",
                    taskNotFoundException
                )
            }
        }
    }

    private fun grantZaakspecifiekeAutorisatieToNewAssignee(task: Task, assignee: String) {
        if (task.assignee == assignee) return
        val zaak = zrcClientService.readZaak(readZaakUUID(task))
        if (zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, assignee)) {
            taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, assignee)
        }
    }

    private fun deleteTaakOpNaamSignaleringenExceptFor(task: Task, assignee: String?) =
        signaleringService.deleteUserSignaleringenExceptFor(
            subjecttype = SignaleringSubject.TAAK,
            subject = task.id,
            types = setOf(SignaleringType.Type.TAAK_OP_NAAM),
            keptUsername = assignee
        )

    private fun sendTaakOpNaamSignalering(task: Task, loggedInUser: LoggedInUser) {
        eventingService.send(signaleringEvent(SignaleringType.Type.TAAK_OP_NAAM, task, loggedInUser))
    }

    private fun sendScreenEventsAndIndex(task: Task) {
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(readZaakUUID(task)))
        indexingService.indexeerDirect(task.id, ZoekObjectType.TAAK, false)
    }

}
