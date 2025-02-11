/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskAfterContext
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskBeforeContext
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskInterceptor
import java.util.Date
import java.util.UUID

class ZacCreateHumanTaskInterceptor : CreateHumanTaskInterceptor {
    companion object {
        const val VAR_TRANSIENT_TAAKDATA: String = "taakdata"
        const val VAR_TRANSIENT_ZAAK_UUID: String = "zaakUUID"
        const val VAR_TRANSIENT_DUE_DATE: String = "dueDate"
        const val VAR_TRANSIENT_DESCRIPTION: String = "description"
        const val VAR_TRANSIENT_CANDIDATE_GROUP: String = "candidateGroupId"
        const val VAR_TRANSIENT_ASSIGNEE: String = "assignee"
        const val VAR_TRANSIENT_OWNER: String = "owner"

        /**
         * This must be lower than the DEFAULT_SUSPENSION_TIMEOUT defined in `websockets.service.ts`
         */
        const val SECONDS_TO_DELAY: Int = 3
    }

    override fun beforeCreateHumanTask(context: CreateHumanTaskBeforeContext) {
        (context.getPlanItemInstanceEntity().getTransientVariable(VAR_TRANSIENT_OWNER) as String?)?.let {
            context.setOwner(it)
        }
        (context.getPlanItemInstanceEntity().getTransientVariable(VAR_TRANSIENT_CANDIDATE_GROUP) as String?)?.let {
            context.setCandidateGroups(listOf(it))
        }
        (context.getPlanItemInstanceEntity().getTransientVariable(VAR_TRANSIENT_ASSIGNEE) as String?)?.let {
            context.setAssignee(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun afterCreateHumanTask(context: CreateHumanTaskAfterContext) {
        (
            context.getPlanItemInstanceEntity()
                .getTransientVariable(VAR_TRANSIENT_TAAKDATA) as Map<String, String>
            ).let { FlowableHelper.getInstance().taakVariabelenService.setTaskData(context.getTaskEntity(), it) }
        (
            context
                .getPlanItemInstanceEntity()
                .getTransientVariable(VAR_TRANSIENT_DUE_DATE) as Date?
            )?.let {
            context.getTaskEntity().dueDate = it
        }
        (
            context
                .getPlanItemInstanceEntity()
                .getTransientVariable(VAR_TRANSIENT_DESCRIPTION) as String?
            )?.let {
            context.getTaskEntity().description = it
        }
        (context.getPlanItemInstanceEntity().getTransientVariable(VAR_TRANSIENT_ZAAK_UUID) as UUID).let {
            val screenEvent = ScreenEventType.ZAAK_TAKEN.updated(it)
            // Wait some time before handling the event to make sure that the task has been created.
            screenEvent.setDelay(SECONDS_TO_DELAY)
            FlowableHelper.getInstance().eventingService.send(screenEvent)
        }
        context.getTaskEntity().assignee?.let {
            // On creation of a human task the event observer will assume its owner is the actor who created it.
            val signaleringEvent = SignaleringEventUtil.event(
                SignaleringType.Type.TAAK_OP_NAAM,
                context.getTaskEntity(),
                null
            )
            // Wait some time before handling the event to make sure that the task has been created.
            signaleringEvent.setDelay(SECONDS_TO_DELAY)
            FlowableHelper.getInstance().eventingService.send(signaleringEvent)
        }
        FlowableHelper.getInstance().indexeerService.addOrUpdateTaak(context.getTaskEntity().id)
    }
}
