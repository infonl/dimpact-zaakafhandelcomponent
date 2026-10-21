/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task

import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.SECONDS_TO_DELAY
import net.atos.zac.websocket.event.ScreenEventType
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
        // Wait some time before handling the event to make sure that the task has been created.
        screenEvent.setDelay(SECONDS_TO_DELAY)
        FlowableHelper.getInstance().eventingService.send(screenEvent)
        FlowableHelper.getInstance().indexeerService.addOrUpdateTaak(context.taskEntity.id)
    }
}
