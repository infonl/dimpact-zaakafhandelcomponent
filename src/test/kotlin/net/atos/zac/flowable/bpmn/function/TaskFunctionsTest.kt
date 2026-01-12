/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.function

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.impl.context.Context
import org.flowable.common.engine.impl.interceptor.CommandContext
import org.flowable.engine.impl.persistence.entity.ExecutionEntity
import org.flowable.engine.impl.util.CommandContextUtil
import org.flowable.identitylink.api.history.HistoricIdentityLink
import org.flowable.task.api.history.HistoricTaskInstance

class TaskFunctionsTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
        clearAllMocks()
    }

    Given("a process instance with historic tasks") {
        val processInstanceId = "proc-123"
        val taskDefinitionKey = "behandelen_taak"
        val assignee = "user-1"
        val groupId = "groupId"
        val candidateType = "candidate"

        When("requesting the behandelaar (assignee)") {
            mockkObject(FlowableHelper)
            val flowableHelper = mockk<FlowableHelper>()
            every { FlowableHelper.getInstance() } returns flowableHelper
            val mockTask = mockk<HistoricTaskInstance>()
            every {
                flowableHelper.flowableHistoryService
                    .createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(taskDefinitionKey)
                    .orderByTaskCreateTime().desc()
                    .list()
            } returns listOf(mockTask)
            every { mockTask.assignee } returns assignee

            mockkStatic(Context::class)
            val commandContext = mockk<CommandContext>()
            every { Context.getCommandContext() } returns commandContext
            val executionEntityMock = mockk<ExecutionEntity>()
            every { executionEntityMock.processInstanceId } returns processInstanceId
            mockkStatic(CommandContextUtil::class)
            every {
                CommandContextUtil.getInvolvedExecutions(any<CommandContext>())
            } returns mapOf("123" to executionEntityMock)

            val user = behandelaar(taskDefinitionKey)
            Then("it should return the correct assignee") {
                user shouldBe "user-1"
            }
        }

        When("requesting the groep (candidate group)") {
            mockkObject(FlowableHelper)
            val flowableHelper = mockk<FlowableHelper>()
            every { FlowableHelper.getInstance() } returns flowableHelper
            val mockTask = mockk<HistoricTaskInstance>()
            every {
                flowableHelper.flowableHistoryService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(taskDefinitionKey)
                    .orderByTaskCreateTime().desc()
                    .list()
            } returns listOf(mockTask)

            mockkStatic(Context::class)
            val commandContext = mockk<CommandContext>()
            every { Context.getCommandContext() } returns commandContext
            val executionEntityMock = mockk<ExecutionEntity>()
            every { executionEntityMock.processInstanceId } returns processInstanceId
            mockkStatic(CommandContextUtil::class)
            every {
                CommandContextUtil.getInvolvedExecutions(any<CommandContext>())
            } returns mapOf("123" to executionEntityMock)

            val identityLink = mockk<HistoricIdentityLink>()
            every { identityLink.type } returns candidateType
            every { identityLink.groupId } returns groupId
            every { mockTask.identityLinks } returns listOf(identityLink)

            val group = groep(taskDefinitionKey)

            Then("it should return the first candidate group ID") {
                group shouldBe groupId
            }
        }

        When("no task exists for the definition key") {
            mockkObject(FlowableHelper)
            val flowableHelper = mockk<FlowableHelper>()
            every { FlowableHelper.getInstance() } returns flowableHelper
            every {
                flowableHelper.flowableHistoryService
                    .createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(taskDefinitionKey)
                    .orderByTaskCreateTime().desc()
                    .list()
            } returns emptyList()

            mockkStatic(Context::class)
            val commandContext = mockk<CommandContext>()
            every { Context.getCommandContext() } returns commandContext
            val executionEntityMock = mockk<ExecutionEntity>()
            every { executionEntityMock.processInstanceId } returns processInstanceId
            mockkStatic(CommandContextUtil::class)
            every {
                CommandContextUtil.getInvolvedExecutions(any<CommandContext>())
            } returns mapOf("123" to executionEntityMock)

            val user = behandelaar(taskDefinitionKey)
            val group = groep(taskDefinitionKey)

            Then("behandelaar and groep should return null") {
                user shouldBe null
                group shouldBe null
            }
        }
    }
})
