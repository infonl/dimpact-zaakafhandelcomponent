package net.atos.zac.task

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.model.RESTTaakToekennenGegevens
import net.atos.zac.app.taken.model.RESTTaakVerdelenGegevens
import net.atos.zac.app.taken.model.RESTTaakVrijgevenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.exception.TaskNotFoundException
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import org.flowable.task.api.Task
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

@AllOpen
class TaskService @Inject constructor(
    private val flowableTaskService: FlowableTaskService,
    private val indexeerService: IndexeerService,
    private val eventingService: EventingService,
    private val restTaakConverter: RESTTaakConverter,
) {
    companion object {
        private val LOG = Logger.getLogger(TaskService::class.java.name)
    }

    fun assignTask(
        restTaakToekennenGegevens: RESTTaakToekennenGegevens,
        task: Task,
        loggedInUser: LoggedInUser
    ) {
        val groupId = restTaakConverter.extractGroupId(task.identityLinks)
        var changed = false
        var updatedTask = task
        restTaakToekennenGegevens.behandelaarId?.let {
            if (groupId != restTaakToekennenGegevens.groepId) {
                flowableTaskService.assignTaskToGroup(
                    updatedTask,
                    restTaakToekennenGegevens.groepId,
                    restTaakToekennenGegevens.reden
                )
                changed = true
            }
            if (task.assignee != it) {
                updatedTask = assignTaskToUser(
                    taskId = task.id,
                    assignee = it,
                    loggedInUser = loggedInUser,
                    explanation = restTaakToekennenGegevens.reden
                )
                changed = true
            }
        }
        if (changed) {
            sendScreenEventsOnTaskChange(updatedTask, restTaakToekennenGegevens.zaakUuid)
            indexeerService.indexeerDirect(
                restTaakToekennenGegevens.taakId,
                ZoekObjectType.TAAK,
                true
            )
        }
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
        @SpanAttribute("restTaakVerdelenGegevens") restTaakVerdelenGegevens: RESTTaakVerdelenGegevens,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null,
    ) {
        LOG.info {
            "Started to assign ${restTaakVerdelenGegevens.taken.size} tasks " +
                "with screen event resource ID: '$screenEventResourceId'."
        }
        val succesfullyAssignedTaskIds = mutableListOf<String>()
        restTaakVerdelenGegevens.taken.forEach { restTaakVerdelenTaak ->
            try {
                flowableTaskService.readOpenTask(restTaakVerdelenTaak.taakId).let { task ->
                    assignTaskAndOptionallyReleaseFromAssignee(task, restTaakVerdelenGegevens, loggedInUser)
                    sendScreenEventsOnTaskChange(task, restTaakVerdelenTaak.zaakUuid)
                }
                succesfullyAssignedTaskIds.add(restTaakVerdelenTaak.taakId)
            } catch (taskNotFoundException: TaskNotFoundException) {
                LOG.log(
                    Level.SEVERE,
                    "No open task with ID '${restTaakVerdelenTaak.taakId}' found. Skipping task.",
                    taskNotFoundException
                )
            }
        }
        indexeerService.indexeerDirect(succesfullyAssignedTaskIds.stream(), ZoekObjectType.TAAK, true)
        LOG.info { "Successfully assigned ${succesfullyAssignedTaskIds.size} tasks." }

        // if a screen event resource ID was specified, send a screen event
        // with the provided job ID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.info { "Sending 'TAKEN_VERDELEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.TAKEN_VERDELEN.updated(it))
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
     * Releases a list of tasks from any assigned user, sends corresponding screen events
     * and updates the search index.
     * This can be a long-running operation.
     */
    @WithSpan
    fun releaseTasks(
        @SpanAttribute("restTaakVerdelenGegevens") restTaakVrijgevenGegevens: RESTTaakVrijgevenGegevens,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null
    ) {
        LOG.info {
            "Started to assign ${restTaakVrijgevenGegevens.taken.size} tasks " +
                "with screen event resource ID: '$screenEventResourceId'."
        }
        val taskIds = mutableListOf<String>()
        restTaakVrijgevenGegevens.taken.forEach {
            flowableTaskService.readOpenTask(it.taakId).let { task ->
                releaseTask(
                    task = task,
                    loggedInUser = loggedInUser,
                    reden = restTaakVrijgevenGegevens.reden
                )
                sendScreenEventsOnTaskChange(task, it.zaakUuid)
                taskIds.add(task.id)
            }
        }
        indexeerService.indexeerDirect(taskIds.stream(), ZoekObjectType.TAAK, true)
        LOG.info { "Successfully released ${taskIds.size} tasks." }

        // if a screen event resource ID was specified, send a screen event
        // with the provided job ID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.info { "Sending 'TAKEN_VRIJGEVEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.TAKEN_VRIJGEVEN.updated(it))
        }
    }

    fun sendScreenEventsOnTaskChange(task: Task, zaakUuid: UUID) {
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(zaakUuid))
    }

    private fun assignTaskAndOptionallyReleaseFromAssignee(
        task: Task,
        restTaakVerdelenGegevens: RESTTaakVerdelenGegevens,
        loggedInUser: LoggedInUser
    ) {
        flowableTaskService.assignTaskToGroup(
            task,
            restTaakVerdelenGegevens.groepId,
            restTaakVerdelenGegevens.reden
        )
        restTaakVerdelenGegevens.behandelaarGebruikersnaam?.run {
            assignTaskToUser(
                taskId = task.id,
                assignee = this,
                loggedInUser = loggedInUser,
                explanation = restTaakVerdelenGegevens.reden
            )
        } ?: run {
            // if no assignee was specified _and_ the task currently has an assignee,
            // only then release it
            task.assignee?.run {
                releaseTask(
                    task = task,
                    loggedInUser = loggedInUser,
                    reden = restTaakVerdelenGegevens.reden
                )
            }
        }
    }

    private fun releaseTask(
        task: Task,
        loggedInUser: LoggedInUser,
        reden: String?
    ) {
        flowableTaskService.releaseTask(task, reden).run {
            eventingService.send(
                SignaleringEventUtil.event(
                    SignaleringType.Type.TAAK_OP_NAAM,
                    this,
                    loggedInUser
                )
            )
        }
    }
}
