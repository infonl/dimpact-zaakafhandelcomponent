package net.atos.zac.task

import jakarta.inject.Inject
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
import org.flowable.task.api.Task
import java.util.UUID
import kotlin.collections.ArrayList

class TaskService @Inject constructor(
    private val flowableTaskService: FlowableTaskService,
    private val indexeerService: IndexeerService,
    private val eventingService: EventingService,
    private val restTaakConverter: RESTTaakConverter
) {
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
     * Assigns a list of tasks to a group and optionally also to a user,
     * sends corresponding screen events and updates the search index.
     */
    fun assignTasks(
        restTaakVerdelenGegevens: RESTTaakVerdelenGegevens,
        loggedInUser: LoggedInUser
    ) {
        val taakIds: MutableList<String> = ArrayList()
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
    }

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
     * Releases a list of tasks, sends corresponding screen events and updated the search index.
     */
    fun releaseTasks(
        restTaakVrijgevenGegevens: RESTTaakVrijgevenGegevens,
        loggedInUser: LoggedInUser
    ) {
        val taakIds = mutableListOf<String>()
        restTaakVrijgevenGegevens.taken.forEach {
            releaseTask(
                taskId = it.taakId,
                loggedInUser = loggedInUser,
                reden = restTaakVrijgevenGegevens.reden
            ).let { updatedTask ->
                sendScreenEventsOnTaskChange(updatedTask, it.zaakUuid)
                taakIds.add(updatedTask.id)
            }
        }
        indexeerService.indexeerDirect(taakIds, ZoekObjectType.TAAK)
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
