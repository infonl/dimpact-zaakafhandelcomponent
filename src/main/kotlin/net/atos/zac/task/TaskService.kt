package net.atos.zac.task

import io.opentelemetry.api.trace.Tracer
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.model.RESTTaakToekennenGegevens
import net.atos.zac.app.taken.model.RESTTaakVerdelenGegevens
import net.atos.zac.app.taken.model.RESTTaakVrijgevenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.opentelemetry.withSpan
import nl.lifely.zac.util.AllOpen
import org.flowable.task.api.Task
import java.util.UUID
import java.util.logging.Logger

@AllOpen
class TaskService @Inject constructor(
    private val flowableTaskService: FlowableTaskService,
    private val indexeerService: IndexeerService,
    private val eventingService: EventingService,
    private val restTaakConverter: RESTTaakConverter,
    private val tracer: Tracer
) {
    companion object {
        private val LOG = Logger.getLogger(TaskService::class.java.name)
    }

    fun assignTask(
        restTaakToekennenGegevens: RESTTaakToekennenGegevens,
        task: Task,
        loggedInUser: LoggedInUser
    ) {
        val groep = restTaakConverter.extractGroupId(task.identityLinks)
        var changed = false
        var updatedTask = task
        restTaakToekennenGegevens.behandelaarId?.let {
            if (groep != restTaakToekennenGegevens.groepId) {
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
            indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK)
        }
    }

    /**
     * Asynchronously assigns a list of tasks to a group and optionally also to a user,
     * sends corresponding screen events and updates the search index.
     */
    suspend fun assignTasksAsync(
        restTaakVerdelenGegevens: RESTTaakVerdelenGegevens,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null,
    ) = withSpan(
        tracer = tracer,
        spanName = "${javaClass.kotlin.simpleName}.assignTasksAsync",
        coroutineContext = Dispatchers.IO
    ) { _ ->
        LOG.fine {
            "Started asynchronous job with ID: $screenEventResourceId to assign " +
                "${restTaakVerdelenGegevens.taken.size} tasks."
        }
        val taakIds = mutableListOf<String>()
        restTaakVerdelenGegevens.taken.forEach { restTaakVerdelenTaak ->
            val task = flowableTaskService.readOpenTask(restTaakVerdelenTaak.taakId)
            flowableTaskService.assignTaskToGroup(
                task,
                restTaakVerdelenGegevens.groepId,
                restTaakVerdelenGegevens.reden
            )
            restTaakVerdelenGegevens.behandelaarGebruikersnaam?.let {
                assignTaskToUser(
                    taskId = task.id,
                    assignee = it,
                    loggedInUser = loggedInUser,
                    explanation = restTaakVerdelenGegevens.reden
                )
            }
            sendScreenEventsOnTaskChange(task, restTaakVerdelenTaak.zaakUuid)
            taakIds.add(restTaakVerdelenTaak.taakId)
        }
        indexeerService.indexeerDirect(taakIds, ZoekObjectType.TAAK)
        LOG.fine {
            "Asynchronous assign tasks job with ID '$screenEventResourceId' finished. " +
                "Successfully assigned ${taakIds.size} tasks."
        }
        // if a screen event resource ID was specified, send a screen event
        // with the provided job ID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            eventingService.send(ScreenEventType.TAKEN_VERDELEN.updated(it))
        }
    }

    /**
     * Assigns a task to a user and sends the 'taak op naam' signalering event.
     */
    /**
     * Assigns a task to a user and sends the 'taak op naam' signalering event.
     */
    fun assignTaskToUser(
        taskId: String,
        assignee: String,
        loggedInUser: LoggedInUser,
        explanation: String?
    ): Task {
        flowableTaskService.assignTaskToUser(taskId, assignee, explanation).let { updatedTask ->
            eventingService.send(
                SignaleringEventUtil.event(
                    SignaleringType.Type.TAAK_OP_NAAM,
                    updatedTask,
                    loggedInUser
                )
            )
            return updatedTask
        }
    }

    /**
     * Asynchronously releases a list of tasks, sends corresponding screen events and updates the search index.
     */
    /**
     * Asynchronously releases a list of tasks, sends corresponding screen events and updates the search index.
     */
    suspend fun releaseTasksAsync(
        restTaakVrijgevenGegevens: RESTTaakVrijgevenGegevens,
        loggedInUser: LoggedInUser,
        screenEventResourceId: String? = null
    ) = withSpan(
        tracer = tracer,
        spanName = "${javaClass.kotlin.simpleName}.releaseTasksAsync",
        coroutineContext = Dispatchers.IO
    ) { _ ->
        LOG.fine {
            "Started asynchronous job with ID: '$screenEventResourceId' to release " +
                "${restTaakVrijgevenGegevens.taken.size} tasks."
        }
        val taskIds = mutableListOf<String>()
        restTaakVrijgevenGegevens.taken.forEach {
            releaseTask(
                taskId = it.taakId,
                loggedInUser = loggedInUser,
                reden = restTaakVrijgevenGegevens.reden
            ).let { updatedTask ->
                sendScreenEventsOnTaskChange(updatedTask, it.zaakUuid)
                taskIds.add(updatedTask.id)
            }
        }
        indexeerService.indexeerDirect(taskIds, ZoekObjectType.TAAK)
        LOG.fine {
            "Asynchronous release tasks job with ID '$screenEventResourceId' finished. " +
                "Successfully released ${taskIds.size} tasks."
        }
        // if a screen event resource ID was specified, send a screen event
        // with the provided job ID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            eventingService.send(ScreenEventType.TAKEN_VRIJGEVEN.updated(it))
        }
    }

    fun sendScreenEventsOnTaskChange(task: Task, zaakUuid: UUID) {
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(zaakUuid))
    }

    private fun releaseTask(
        taskId: String,
        loggedInUser: LoggedInUser,
        reden: String?
    ): Task {
        flowableTaskService.releaseTask(taskId, reden).let { updatedTask ->
            eventingService.send(
                SignaleringEventUtil.event(
                    SignaleringType.Type.TAAK_OP_NAAM,
                    updatedTask,
                    loggedInUser
                )
            )
            return updatedTask
        }
    }
}
