/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.flowable.log.UserTaskCompleted
import nl.info.zac.log.Employee
import nl.info.zac.log.ZacLogger
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent
import org.flowable.common.engine.api.delegate.event.FlowableEvent
import org.flowable.common.engine.api.delegate.event.FlowableEventListener
import org.flowable.engine.delegate.TransactionDependentExecutionListener
import org.flowable.task.service.impl.persistence.entity.TaskEntity

@ApplicationScoped
class UserTaskCompletionListener : FlowableEventListener {
    companion object {
        private val LOG = ZacLogger.getLogger(UserTaskCompletionListener::class)
    }

    override fun onEvent(event: FlowableEvent) {
        when (event.type) {
            FlowableEngineEventType.TASK_COMPLETED ->
                removeTaak(event as FlowableEntityEvent)
        }
    }

    private fun removeTaak(entityEvent: FlowableEntityEvent) {
        (entityEvent.entity as TaskEntity).let { task ->
            LOG.fine {
                UserTaskCompleted(
                    taakId = task.id,
                    taskName = task.name,
                    processInstanceId = task.processInstanceId,
                    processDefinitionId = task.processDefinitionId,
                    assignee = task.assignee?.let(::Employee)
                )
            }
            FlowableHelper.getInstance().indexeerService.removeTaak(task.id)
        }
    }

    override fun isFailOnException() = true

    override fun isFireOnTransactionLifecycleEvent() = true

    override fun getOnTransaction(): String = TransactionDependentExecutionListener.ON_TRANSACTION_COMMITTED
}
