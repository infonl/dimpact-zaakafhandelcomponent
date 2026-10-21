/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskAfterContext
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskBeforeContext
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskInterceptor
import org.flowable.common.engine.impl.cfg.TransactionState
import org.flowable.common.engine.impl.context.Context
import java.util.Date
import java.util.UUID

/**
 * Custom Flowable create human task interceptor.
 */
@NoArgConstructor
class ZacCreateHumanTaskInterceptor : CreateHumanTaskInterceptor {
    companion object {
        const val VAR_TRANSIENT_TAAKDATA = "taakdata"
        const val VAR_TRANSIENT_ZAAK_UUID = "zaakUUID"
        const val VAR_TRANSIENT_DUE_DATE = "dueDate"
        const val VAR_TRANSIENT_DESCRIPTION = "description"
        const val VAR_TRANSIENT_CANDIDATE_GROUP = "candidateGroupId"
        const val VAR_TRANSIENT_ASSIGNEE = "assignee"
        const val VAR_TRANSIENT_OWNER = "owner"
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
        val screenEvent = ScreenEventType.ZAAK_TAKEN.updated(
            context.getPlanItemInstanceEntity().getTransientVariable(VAR_TRANSIENT_ZAAK_UUID) as UUID
        )
        // On creation of a human task the event observer will assume its owner is the actor who created it.
        val signaleringEvent = context.getTaskEntity().assignee?.let {
            SignaleringEventUtil.event(SignaleringType.Type.TAAK_OP_NAAM, context.getTaskEntity(), null)
        }
        // The handlers of these events read the task, which only exists for them once Flowable has committed
        // the transaction. The task listeners in the same transaction, such as the one of the
        // aanvullende informatie task, call external APIs, so no fixed delay is long enough.
        Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED) {
            with(FlowableHelper.getInstance().eventingService) {
                send(screenEvent)
                signaleringEvent?.let(::send)
            }
        }
        FlowableHelper.getInstance().indexeerService.addOrUpdateTaak(context.getTaskEntity().id)
    }
}
