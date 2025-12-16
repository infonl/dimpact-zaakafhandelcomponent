/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.zgw.zrc.util.isOpen
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent
import org.flowable.common.engine.api.delegate.event.FlowableEvent
import org.flowable.common.engine.api.delegate.event.FlowableEventListener
import org.flowable.engine.delegate.TransactionDependentExecutionListener
import org.flowable.engine.runtime.ProcessInstance
import org.flowable.task.service.impl.persistence.entity.TaskEntity
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
class UserTaskCompletionListener : FlowableEventListener {
    companion object {
        private val LOG = Logger.getLogger(UserTaskCompletionListener::class.java.name)
        private const val EINDSTATUS_TOELICHTING = "Zaak beeindigd"
    }

    override fun onEvent(event: FlowableEvent) {
        when (event.type) {
            FlowableEngineEventType.TASK_COMPLETED ->
                removeTaak(event as FlowableEntityEvent)
            FlowableEngineEventType.PROCESS_COMPLETED, FlowableEngineEventType.PROCESS_CANCELLED ->
                closeZaak(event as FlowableEntityEvent)
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

    private fun closeZaak(entityEvent: FlowableEntityEvent) {
        (entityEvent.entity as ProcessInstance).let { processInstance ->
            LOG.fine(
                "Process with id '${processInstance.processInstanceId}'," +
                    " name '${processInstance.processDefinitionName} " +
                    "is in state ${entityEvent.type}'"
            )
            processInstance.processVariables["ZK_Result"]?.let { resultaatTypeOmschrijving ->
                val zaakUUID = UUID.fromString(processInstance.businessKey)
                val flowableHelper = FlowableHelper.getInstance()
                val zaak = flowableHelper.zrcClientService.readZaak(zaakUUID)

                if (zaak.isOpen()) {
                    try {
                        flowableHelper.zgwApiService.endZaak(
                            zaakUUID,
                            resultaatTypeOmschrijving.toString(),
                            EINDSTATUS_TOELICHTING
                        )
                    } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                        // rethrow as a FlowableException
                        // just to ensure that it is logged in [CommandContext] at log level INFO instead of ERROR
                        throw FlowableZgwValidationErrorException("Failed to end zaak", zgwValidationErrorException)
                    }
                } else {
                    LOG.fine(
                        "Zaak '$zaakUUID' is already closed, skipping close operation in process completion listener"
                    )
                }
            }
        }
    }

    override fun isFailOnException() = true

    override fun isFireOnTransactionLifecycleEvent() = true

    override fun getOnTransaction(): String = TransactionDependentExecutionListener.ON_TRANSACTION_COMMITTED
}
