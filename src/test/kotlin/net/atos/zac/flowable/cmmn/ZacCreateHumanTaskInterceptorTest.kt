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
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_DESCRIPTION
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_DUE_DATE
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_TAAKDATA
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_ZAAK_UUID
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.event.EventingService
import nl.info.zac.flowable.task.TaakVariabelenService
import nl.info.zac.search.IndexingService
import nl.info.zac.websocket.event.ScreenEvent
import nl.info.zac.websocket.event.ScreenEventType
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
    val zaakUuid = UUID.randomUUID()

    val zacCreateHumanTaskInterceptor = ZacCreateHumanTaskInterceptor()

    mockkObject(FlowableHelper)
    mockkStatic(Context::class)

    fun stubCreatedHumanTask(assignee: String?) {
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.taakVariabelenService } returns taakVariabelenService
        every { flowableHelper.indexeerService } returns indexingService
        every { createHumanTaskAfterContext.planItemInstanceEntity } returns planItemInstanceEntity
        every { createHumanTaskAfterContext.taskEntity } returns taskEntity
        with(planItemInstanceEntity) {
            every { getTransientVariable(VAR_TRANSIENT_TAAKDATA) } returns emptyMap<String, String>()
            every { getTransientVariable(VAR_TRANSIENT_DUE_DATE) } returns null
            every { getTransientVariable(VAR_TRANSIENT_DESCRIPTION) } returns null
            every { getTransientVariable(VAR_TRANSIENT_ZAAK_UUID) } returns zaakUuid
        }
        every { taskEntity.id } returns taskId
        every { taskEntity.assignee } returns assignee
        every { Context.getTransactionContext() } returns transactionContext
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
        every {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, capture(transactionListenerSlot))
        } returns Unit

        `when`("the human task has been created, but the Flowable transaction is not yet committed") {
            zacCreateHumanTaskInterceptor.afterCreateHumanTask(createHumanTaskAfterContext)

            then(
                """
                no zaak taken screen event and no taak op naam signalering event are sent yet, so that their
                handlers never read a task that does not exist yet for them
                """
            ) {
                verify(exactly = 0) {
                    eventingService.send(any<ScreenEvent>())
                    eventingService.send(any<SignaleringEvent<*>>())
                }
            }
        }

        `when`("the Flowable transaction is committed") {
            every { flowableHelper.eventingService } returns eventingService
            zacCreateHumanTaskInterceptor.afterCreateHumanTask(createHumanTaskAfterContext)
            transactionListenerSlot.captured.execute(mockk<CommandContext>())

            then("a zaak taken screen event for the zaak is sent") {
                val screenEventSlot = slot<ScreenEvent>()
                verify(exactly = 1) { eventingService.send(capture(screenEventSlot)) }
                with(screenEventSlot.captured) {
                    objectType shouldBe ScreenEventType.ZAAK_TAKEN
                    objectId.resource shouldBe zaakUuid.toString()
                }
            }

            and("a taak op naam signalering event for the task is sent") {
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
        val transactionListenerSlot = slot<TransactionListener>()
        stubCreatedHumanTask(assignee = null)
        every {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, capture(transactionListenerSlot))
        } returns Unit

        `when`("the Flowable transaction is committed") {
            every { flowableHelper.eventingService } returns eventingService
            zacCreateHumanTaskInterceptor.afterCreateHumanTask(createHumanTaskAfterContext)
            transactionListenerSlot.captured.execute(mockk<CommandContext>())

            then("only a zaak taken screen event is sent") {
                verify(exactly = 1) { eventingService.send(any<ScreenEvent>()) }
                verify(exactly = 0) { eventingService.send(any<SignaleringEvent<*>>()) }
            }
        }
    }
})
