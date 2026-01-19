/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent
import org.flowable.common.engine.api.delegate.event.FlowableEvent
import org.flowable.common.engine.api.delegate.event.FlowableEventListener
import org.flowable.engine.delegate.TransactionDependentExecutionListener
import org.flowable.task.service.impl.persistence.entity.TaskEntity
import java.util.logging.Logger

@ApplicationScoped
class UserTaskCompletionListener : FlowableEventListener {
    companion object {
        private val LOG = Logger.getLogger(UserTaskCompletionListener::class.java.name)
    }

    override fun onEvent(event: FlowableEvent) {
        when (event.type) {
            FlowableEngineEventType.TASK_COMPLETED ->
                removeTaak(event as FlowableEntityEvent)
        }
    }

    private fun removeTaak(entityEvent: FlowableEntityEvent) {
        (entityEvent.entity as TaskEntity).let { task ->
            LOG.fine(
                "User task with id '${task.id}, name '${task.name}' completed for process id " +
                    "'${task.processInstanceId}', process name '${task.processDefinitionId} " +
                    "by user '${task.assignee}'"
            )
            FlowableHelper.getInstance().indexeerService.removeTaak(task.id)
        }
    }

    override fun isFailOnException() = true

    override fun isFireOnTransactionLifecycleEvent() = true

    override fun getOnTransaction(): String = TransactionDependentExecutionListener.ON_TRANSACTION_COMMITTED
}
