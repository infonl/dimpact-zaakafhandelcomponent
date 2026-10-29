/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task

import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import nl.info.zac.websocket.event.ScreenEventType
import org.flowable.common.engine.impl.cfg.TransactionState
import org.flowable.common.engine.impl.context.Context
import org.flowable.engine.interceptor.CreateUserTaskAfterContext
import org.flowable.engine.interceptor.CreateUserTaskBeforeContext
import org.flowable.engine.interceptor.CreateUserTaskInterceptor
import java.util.UUID

class ZacCreateUserTaskInterceptor : CreateUserTaskInterceptor {
    companion object {
        const val VAR_PROCESS_OWNER = "owner"
    }

    override fun beforeCreateUserTask(context: CreateUserTaskBeforeContext) {
        (context.execution.getVariable(VAR_PROCESS_OWNER) as String?)?.let(context::setOwner)
    }

    override fun afterCreateUserTask(context: CreateUserTaskAfterContext) {
        val zaakUUID = context.execution.parent.getVariable(VAR_ZAAK_UUID) as UUID
        val screenEvent = ScreenEventType.ZAAK_TAKEN.updated(zaakUUID)
        val task = context.taskEntity
        val flowableHelper = FlowableHelper.getInstance()
        task.assignee?.let { assignee ->
            flowableHelper.zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(
                zaak = flowableHelper.zrcClientService.readZaak(zaakUUID),
                medewerkerId = assignee,
                task = task
            )
        }
        // The zaak page reloads its taken on this event, and the task only exists for it once Flowable has
        // committed the transaction, which can take longer than any fixed delay.
        Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED) {
            flowableHelper.eventingService.send(screenEvent)
        }
        flowableHelper.indexeerService.addOrUpdateTaak(task.id)
    }
}
