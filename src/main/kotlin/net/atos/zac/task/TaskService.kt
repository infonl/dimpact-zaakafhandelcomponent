/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.task

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.zac.app.task.model.RestTaskAssignData
import net.atos.zac.app.task.model.RestTaskDistributeData
import net.atos.zac.app.task.model.RestTaskDistributeTask
import net.atos.zac.app.task.model.RestTaskReleaseData
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.exception.TaskNotFoundException
import net.atos.zac.search.IndexingService
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.AllOpen
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
) {
    companion object {
        private val LOG = Logger.getLogger(TaskService::class.java.name)
    }

    fun assignOrReleaseTask(
        restTaskAssignData: RestTaskAssignData,
        task: Task,
        loggedInUser: LoggedInUser
    ) {
        assignTasks(
            RestTaskDistributeData(
                taken = listOf(
                    RestTaskDistributeTask(
                        taakId = task.id,
                        zaakUuid = restTaskAssignData.zaakUuid
                    )
                ),
                groepId = restTaskAssignData.groepId,
                reden = restTaskAssignData.reden,
                behandelaarGebruikersnaam = restTaskAssignData.behandelaarId
            ),
            loggedInUser,
            mutableListOf<String>()
        )
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
     */
    fun assignTaskToUser(
        taskId: String,
        assignee: String,
        loggedInUser: LoggedInUser,
        explanation: String?
    ): Task = flowableTaskService.assignTaskToUser(taskId, assignee, explanation).let { updatedTask ->
        eventingService.send(
            SignaleringEventUtil.event(
                SignaleringType.Type.TAAK_OP_NAAM,
                updatedTask,
                loggedInUser
            )
        )
        updatedTask
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

    fun sendScreenEventsOnTaskChange(task: Task, zaakUuid: UUID) {
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
                flowableTaskService.readOpenTask(restTask.taakId).let {
                    assignTaskAndOptionallyReleaseFromAssignee(it, restTaskDistributeData, loggedInUser)
                    sendScreenEventsOnTaskChange(it, restTask.zaakUuid)
                }
                indexingService.indexeerDirect(restTask.taakId, ZoekObjectType.TAAK, false)
                successfullyAssignedTaskIds.add(restTask.taakId)
            } catch (taskNotFoundException: TaskNotFoundException) {
                // continue assigning remaining tasks if particular open task could not be found
                LOG.log(
                    Level.SEVERE,
                    "No open task with ID '${restTask.taakId}' found while assigning tasks. Skipping task.",
                    taskNotFoundException
                )
            }
        }
    }

    private fun assignTaskAndOptionallyReleaseFromAssignee(
        task: Task,
        restTaskDistributeData: RestTaskDistributeData,
        loggedInUser: LoggedInUser
    ) {
        flowableTaskService.assignTaskToGroup(
            task,
            restTaskDistributeData.groepId,
            restTaskDistributeData.reden
        )
        restTaskDistributeData.behandelaarGebruikersnaam?.run {
            assignTaskToUser(
                taskId = task.id,
                assignee = this,
                loggedInUser = loggedInUser,
                explanation = restTaskDistributeData.reden
            )
        } ?: run {
            // if no assignee was specified _and_ the task currently has an assignee,
            // only then release it
            task.assignee?.run {
                releaseTask(
                    task = task,
                    loggedInUser = loggedInUser,
                    reden = restTaskDistributeData.reden
                )
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
                    val updatedTask = releaseTask(
                        task = task,
                        loggedInUser = loggedInUser,
                        reden = restTaskReleaseData.reden
                    )
                    indexingService.indexeerDirect(task.id, ZoekObjectType.TAAK, false)
                    sendScreenEventsOnTaskChange(updatedTask, it.zaakUuid)
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
