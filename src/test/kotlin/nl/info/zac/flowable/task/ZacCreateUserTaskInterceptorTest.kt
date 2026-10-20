/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.flowable.task.ZacCreateUserTaskInterceptor.Companion.VAR_PROCESS_OWNER
import nl.info.zac.search.IndexingService
import org.flowable.common.engine.impl.cfg.TransactionContext
import org.flowable.common.engine.impl.cfg.TransactionListener
import org.flowable.common.engine.impl.cfg.TransactionState
import org.flowable.common.engine.impl.context.Context
import org.flowable.common.engine.impl.interceptor.CommandContext
import org.flowable.engine.interceptor.CreateUserTaskAfterContext
import org.flowable.engine.interceptor.CreateUserTaskBeforeContext
import java.util.UUID

class ZacCreateUserTaskInterceptorTest : BehaviorSpec({
    val flowableHelper = mockk<FlowableHelper>()
    val eventingService = mockk<EventingService>()
    val indexingService = mockk<IndexingService>()

    val zacCreateUserTaskInterceptor = ZacCreateUserTaskInterceptor()

    mockkObject(FlowableHelper)
    mockkStatic(Context::class)

    afterSpec {
        unmockkAll()
    }

    given("a user task that is about to be created in a process with an owner") {
        val createUserTaskBeforeContext = mockk<CreateUserTaskBeforeContext>()
        every { createUserTaskBeforeContext.execution.getVariable(VAR_PROCESS_OWNER) } returns "fakeOwner"
        every { createUserTaskBeforeContext.owner = "fakeOwner" } returns Unit

        `when`("the interceptor is called before the user task is created") {
            zacCreateUserTaskInterceptor.beforeCreateUserTask(createUserTaskBeforeContext)

            then("the owner of the process becomes the owner of the user task") {
                verify(exactly = 1) { createUserTaskBeforeContext.owner = "fakeOwner" }
            }
        }
    }

    given("a user task that is about to be created in a process without an owner") {
        val createUserTaskBeforeContext = mockk<CreateUserTaskBeforeContext>()
        every { createUserTaskBeforeContext.execution.getVariable(VAR_PROCESS_OWNER) } returns null

        `when`("the interceptor is called before the user task is created") {
            zacCreateUserTaskInterceptor.beforeCreateUserTask(createUserTaskBeforeContext)

            then("no owner is set on the user task") {
                verify(exactly = 0) { createUserTaskBeforeContext.owner = any() }
            }
        }
    }

    given("a user task that has been created in a process of a zaak") {
        val zaakUuid = UUID.randomUUID()
        val createUserTaskAfterContext = mockk<CreateUserTaskAfterContext>()
        val transactionContext = mockk<TransactionContext>()
        val transactionListenerSlot = slot<TransactionListener>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.indexeerService } returns indexingService
        every { Context.getTransactionContext() } returns transactionContext
        every {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, capture(transactionListenerSlot))
        } returns Unit
        every { createUserTaskAfterContext.execution.parent.getVariable(VAR_ZAAK_UUID) } returns zaakUuid
        every { createUserTaskAfterContext.taskEntity.id } returns "fakeTaskId"
        every { indexingService.addOrUpdateTaak("fakeTaskId") } returns Unit

        `when`("the interceptor is called after the user task is created, but before the Flowable commit") {
            zacCreateUserTaskInterceptor.afterCreateUserTask(createUserTaskAfterContext)

            then(
                """
                the user task is indexed, but no zaak taken screen event is sent yet, so that the zaak page never
                reloads its taken before the user task exists for it
                """
            ) {
                verify(exactly = 1) { indexingService.addOrUpdateTaak("fakeTaskId") }
                verify(exactly = 0) { eventingService.send(any<ScreenEvent>()) }
            }
        }

        `when`("the Flowable transaction is committed") {
            val screenEventSlot = slot<ScreenEvent>()
            every { flowableHelper.eventingService } returns eventingService
            every { eventingService.send(capture(screenEventSlot)) } returns Unit

            transactionListenerSlot.captured.execute(mockk<CommandContext>())

            then("a zaak taken screen event for the zaak is sent") {
                with(screenEventSlot.captured) {
                    objectType shouldBe ScreenEventType.ZAAK_TAKEN
                    objectId.resource shouldBe zaakUuid.toString()
                }
            }
        }
    }
})
