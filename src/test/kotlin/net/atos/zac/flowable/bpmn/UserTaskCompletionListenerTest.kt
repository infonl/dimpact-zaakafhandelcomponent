/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import nl.info.zac.search.IndexingService
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent
import org.flowable.common.engine.api.delegate.event.FlowableEvent
import org.flowable.engine.delegate.TransactionDependentExecutionListener
import org.flowable.task.service.impl.persistence.entity.TaskEntity

class UserTaskCompletionListenerTest : BehaviorSpec({
    mockkObject(FlowableHelper.FlowableHelperProvider)

    val fakeFlowableHelper = mockk<FlowableHelper>()
    val fakeIndexingService = mockk<IndexingService>()

    afterContainer {
        unmockkObject(FlowableHelper.FlowableHelperProvider)
    }

    val listener = UserTaskCompletionListener()

    context("onEvent with TASK_COMPLETED event") {
        given("A TASK_COMPLETED FlowableEntityEvent with a TaskEntity") {
            val fakeTaskId = "fakeTaskId1"
            val fakeTaskEntity = mockk<TaskEntity> {
                every { id } returns fakeTaskId
                every { name } returns "fakeTaskName"
                every { processInstanceId } returns "fakeProcessInstanceId"
                every { processDefinitionId } returns "fakeProcessDefinitionId"
                every { assignee } returns "fakeAssignee"
            }
            val fakeEvent = mockk<FlowableEntityEvent> {
                every { type } returns FlowableEngineEventType.TASK_COMPLETED
                every { entity } returns fakeTaskEntity
            }

            every { FlowableHelper.getInstance() } returns fakeFlowableHelper
            every { fakeFlowableHelper.indexeerService } returns fakeIndexingService
            every { fakeIndexingService.removeTaak(fakeTaskId) } returns Unit

            `when`("onEvent is called") {
                listener.onEvent(fakeEvent)

                then("IndexingService.removeTaak is called with the task ID") {
                    verify { fakeIndexingService.removeTaak(fakeTaskId) }
                }
            }
        }
    }

    context("onEvent with non-TASK_COMPLETED event") {
        given("A FlowableEvent of type TASK_CREATED") {
            val fakeEvent = mockk<FlowableEvent> {
                every { type } returns FlowableEngineEventType.TASK_CREATED
            }

            `when`("onEvent is called") {
                listener.onEvent(fakeEvent)

                then("IndexingService.removeTaak is not called") {
                    verify(exactly = 0) { fakeIndexingService.removeTaak(any()) }
                }
            }
        }
    }

    context("Lifecycle method return values") {
        given("The UserTaskCompletionListener instance") {
            `when`("isFailOnException is called") {
                val result = listener.isFailOnException()

                then("true is returned") {
                    result shouldBe true
                }
            }

            `when`("isFireOnTransactionLifecycleEvent is called") {
                val result = listener.isFireOnTransactionLifecycleEvent()

                then("true is returned") {
                    result shouldBe true
                }
            }

            `when`("getOnTransaction is called") {
                val result = listener.onTransaction

                then("ON_TRANSACTION_COMMITTED is returned") {
                    result shouldBe TransactionDependentExecutionListener.ON_TRANSACTION_COMMITTED
                }
            }
        }
    }
})
