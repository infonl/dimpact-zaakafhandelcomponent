package net.atos.zac.task

import jakarta.inject.Inject
import jakarta.validation.Valid
import net.atos.zac.app.taken.model.RESTTaakVerdelenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.flowable.task.api.Task
import java.util.*
import kotlin.collections.ArrayList

class TaskService @Inject constructor(
    private val flowableTaskService: FlowableTaskService,
    private val indexeerService: IndexeerService,
    private val eventingService: EventingService
) {
    /**
     * Asynchronously assigns a list of zaken to a group and/or user and updates the search index on the fly.
     */
    @Suppress("LongParameterList")
    fun assignTasksAsync(
        @Valid restTaakVerdelenGegevens: RESTTaakVerdelenGegevens,
        loggedInUser: LoggedInUser
    ) {
        val taakIds: MutableList<String?> = ArrayList()
        restTaakVerdelenGegevens.taken.forEach { restTaakVerdelenTaak ->
            var task = flowableTaskService.readOpenTask(restTaakVerdelenTaak.taakId)
            restTaakVerdelenGegevens.behandelaarGebruikersnaam?.let {
                task = assignTaskToUser(
                    taskId = task.id,
                    assignee = it,
                    loggedInUser = loggedInUser,
                    explanation = restTaakVerdelenGegevens.reden
                )
            }
            val updatedTask = flowableTaskService.assignTaskToGroup(
                task,
                restTaakVerdelenGegevens.groepId,
                restTaakVerdelenGegevens.reden
            )
            taakBehandelaarGewijzigd(updatedTask, restTaakVerdelenTaak.zaakUuid)
            taakIds.add(restTaakVerdelenTaak.taakId)
        }
        indexeerService.indexeerDirect(taakIds, ZoekObjectType.TAAK)
    }

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

    fun releaseTask(
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

    fun taakBehandelaarGewijzigd(task: Task, zaakUuid: UUID) {
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(zaakUuid))
    }
}
