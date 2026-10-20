/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_DESCRIPTION
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_DUE_DATE
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_TAAKDATA
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_ZAAK_UUID
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.flowable.task.TaakVariabelenService
import nl.info.zac.search.IndexingService
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskAfterContext
import org.flowable.common.engine.impl.cfg.TransactionContext
import org.flowable.common.engine.impl.cfg.TransactionListener
import org.flowable.common.engine.impl.cfg.TransactionState
import org.flowable.common.engine.impl.context.Context
import org.flowable.common.engine.impl.interceptor.CommandContext
import org.flowable.task.service.impl.persistence.entity.TaskEntity
import java.util.UUID

class ZacCreateHumanTaskInterceptorTest : BehaviorSpec({
    val flowableHelper = mockk<FlowableHelper>()
    val eventingService = mockk<EventingService>(relaxed = true)
    val taakVariabelenService = mockk<TaakVariabelenService>(relaxed = true)
    val indexingService = mockk<IndexingService>(relaxed = true)
    val transactionContext = mockk<TransactionContext>()
    val planItemInstanceEntity = mockk<PlanItemInstanceEntity>()
    val taskEntity = mockk<TaskEntity>()
    val createHumanTaskAfterContext = mockk<CreateHumanTaskAfterContext>()
    val taskId = "fakeTaskId"

    val zacCreateHumanTaskInterceptor = ZacCreateHumanTaskInterceptor()

    mockkObject(FlowableHelper)
    mockkStatic(Context::class)

    fun stubCreatedHumanTask(assignee: String?) {
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.eventingService } returns eventingService
        every { flowableHelper.taakVariabelenService } returns taakVariabelenService
        every { flowableHelper.indexeerService } returns indexingService
        every { createHumanTaskAfterContext.planItemInstanceEntity } returns planItemInstanceEntity
        every { createHumanTaskAfterContext.taskEntity } returns taskEntity
        with(planItemInstanceEntity) {
            every { getTransientVariable(VAR_TRANSIENT_TAAKDATA) } returns emptyMap<String, String>()
            every { getTransientVariable(VAR_TRANSIENT_DUE_DATE) } returns null
            every { getTransientVariable(VAR_TRANSIENT_DESCRIPTION) } returns null
            every { getTransientVariable(VAR_TRANSIENT_ZAAK_UUID) } returns UUID.randomUUID()
        }
        every { taskEntity.id } returns taskId
        every { taskEntity.assignee } returns assignee
    }

    afterContainer {
        clearMocks(eventingService, transactionContext, answers = false)
    }

    afterSpec {
        unmockkAll()
    }

    given("a human task that is created with an assignee") {
        val transactionListenerSlot = slot<TransactionListener>()
        stubCreatedHumanTask(assignee = "fakeAssignee")
        every { Context.getTransactionContext() } returns transactionContext
        every {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, capture(transactionListenerSlot))
        } returns Unit

        `when`("the human task has been created, but the Flowable transaction is not yet committed") {
            zacCreateHumanTaskInterceptor.afterCreateHumanTask(createHumanTaskAfterContext)

            then(
                """
                no taak op naam signalering event is sent yet, so that the event observer never reads a task
                that does not exist yet for it
                """
            ) {
                verify(exactly = 0) { eventingService.send(any<SignaleringEvent<*>>()) }
            }
        }

        `when`("the Flowable transaction is committed") {
            zacCreateHumanTaskInterceptor.afterCreateHumanTask(createHumanTaskAfterContext)
            transactionListenerSlot.captured.execute(mockk<CommandContext>())

            then("a taak op naam signalering event for the task is sent") {
                val signaleringEventSlot = slot<SignaleringEvent<*>>()
                verify(exactly = 1) { eventingService.send(capture(signaleringEventSlot)) }
                with(signaleringEventSlot.captured) {
                    objectType shouldBe SignaleringType.Type.TAAK_OP_NAAM
                    objectId.resource() shouldBe taskId
                    actor shouldBe null
                }
            }
        }
    }

    given("a human task that is created without an assignee") {
        stubCreatedHumanTask(assignee = null)

        `when`("the human task has been created") {
            zacCreateHumanTaskInterceptor.afterCreateHumanTask(createHumanTaskAfterContext)

            then("no transaction listener is registered and no signalering event is sent") {
                verify(exactly = 0) {
                    transactionContext.addTransactionListener(any(), any())
                    eventingService.send(any<SignaleringEvent<*>>())
                }
            }
        }
    }
})
