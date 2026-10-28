/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.task.model.RestTaskDistributeData
import nl.info.zac.app.task.model.RestTaskReleaseData
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
            screenEventResourceId?.let {
                LOG.fine { "Sending 'TAKEN_VRIJGEVEN' screen event with ID '$it'." }
                eventingService.send(ScreenEventType.TAKEN_VRIJGEVEN.updated(it))
            }
        }
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
                    groupId = restTaskDistributeData.groepId,
                    username = restTaskDistributeData.behandelaarGebruikersnaam,
                    reason = restTaskDistributeData.reden,
                    loggedInUser = loggedInUser
                )
                successfullyAssignedTaskIds.add(restTask.taakId)
            } catch (taskNotFoundException: TaskNotFoundException) {
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
                    releaseTask(task = task, reason = restTaskReleaseData.reden, loggedInUser = loggedInUser)
                    sendScreenEventsAndIndex(task)
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
