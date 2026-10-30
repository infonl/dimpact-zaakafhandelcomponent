/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeProcessBinding
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.cmmn.CmmnService
import org.flowable.cmmn.api.repository.CaseDefinition
import org.flowable.cmmn.model.HumanTask
import org.flowable.cmmn.model.UserEventListener
import org.flowable.engine.repository.ProcessDefinition

class RestProcessDefinitionConverterTest : BehaviorSpec({
    val cmmnService = mockk<CmmnService>()
    val bpmnService = mockk<BpmnService>()
    val converter = RestProcessDefinitionConverter(cmmnService, bpmnService)

    fun createCaseDefinition() = mockk<CaseDefinition> {
        every { name } returns "fakeCaseName"
        every { key } returns "fakeCaseKey"
    }

    fun createProcessDefinition() = mockk<ProcessDefinition> {
        every { name } returns "fakeProcessName"
        every { key } returns "fakeProcessKey"
        every { isSuspended } returns false
    }

    afterEach { checkUnnecessaryStub() }

    given("a CMMN case definition with a human task and a user event listener, and a BPMN process definition") {
        val caseDefinition = createCaseDefinition().apply { every { id } returns "fakeCaseDefinitionId" }
        val humanTask = mockk<HumanTask> {
            every { id } returns "fakeHumanTaskId"
            every { name } returns "fakeHumanTaskName"
        }
        val userEventListener = mockk<UserEventListener> {
            every { id } returns "fakeUserEventListenerId"
            every { name } returns "fakeUserEventListenerName"
        }
        every { cmmnService.listCaseDefinitions() } returns listOf(caseDefinition)
        every { bpmnService.listProcessDefinitions() } returns listOf(createProcessDefinition())
        every { cmmnService.listHumanTasks("fakeCaseDefinitionId") } returns listOf(humanTask)
        every { cmmnService.listUserEventListeners("fakeCaseDefinitionId") } returns listOf(userEventListener)

        `when`("the process definitions are listed") {
            val processDefinitions = converter.listProcessDefinitions()

            then("both are listed with the engine that runs them") {
                processDefinitions.map { Triple(it.processEngine, it.key, it.naam) } shouldBe listOf(
                    Triple(ProcessEngine.CMMN, "fakeCaseKey", "fakeCaseName"),
                    Triple(ProcessEngine.BPMN, "fakeProcessKey", "fakeProcessName")
                )
            }

            and("the case definition lists its plan items") {
                with(processDefinitions.first()) {
                    humanTaskDefinitions.single().id shouldBe "fakeHumanTaskId"
                    humanTaskDefinitions.single().type shouldBe PlanItemType.HUMAN_TASK
                    userEventListenerDefinitions.single().id shouldBe "fakeUserEventListenerId"
                    userEventListenerDefinitions.single().type shouldBe PlanItemType.USER_EVENT_LISTENER
                }
            }

            and("the BPMN process definition has no plan items") {
                processDefinitions.last().humanTaskDefinitions.shouldBeEmpty()
                processDefinitions.last().userEventListenerDefinitions.shouldBeEmpty()
            }
        }
    }

    given("a suspended BPMN process definition") {
        every { cmmnService.listCaseDefinitions() } returns emptyList()
        every { bpmnService.listProcessDefinitions() } returns listOf(
            mockk<ProcessDefinition> { every { isSuspended } returns true }
        )

        `when`("the process definitions are listed") {
            val processDefinitions = converter.listProcessDefinitions()

            then("it is not listed, because a zaak cannot start a suspended process") {
                processDefinitions.shouldBeEmpty()
            }
        }
    }

    given("a configuration that is bound to a CMMN case definition") {
        val processBinding = ZaaktypeProcessBinding().apply {
            processEngine = ProcessEngine.CMMN
            definitionKey = "fakeCaseKey"
        }
        every { cmmnService.readCaseDefinition("fakeCaseKey") } returns createCaseDefinition()

        `when`("its process definition is converted without relations") {
            val restProcessDefinition = converter.toRestProcessDefinition(processBinding, false)

            then("it names the case definition, without its plan items") {
                restProcessDefinition.processEngine shouldBe ProcessEngine.CMMN
                restProcessDefinition.key shouldBe "fakeCaseKey"
                restProcessDefinition.naam shouldBe "fakeCaseName"
                restProcessDefinition.humanTaskDefinitions.shouldBeEmpty()
            }
        }
    }

    given("a configuration that is bound to a BPMN process definition") {
        val processBinding = ZaaktypeProcessBinding().apply {
            processEngine = ProcessEngine.BPMN
            definitionKey = "fakeProcessKey"
        }
        every {
            bpmnService.findProcessDefinitionByProcessDefinitionKey("fakeProcessKey")
        } returns mockk<ProcessDefinition> { every { name } returns "fakeProcessName" }

        `when`("its process definition is converted") {
            val restProcessDefinition = converter.toRestProcessDefinition(processBinding, true)

            then("it names the process definition") {
                restProcessDefinition.processEngine shouldBe ProcessEngine.BPMN
                restProcessDefinition.key shouldBe "fakeProcessKey"
                restProcessDefinition.naam shouldBe "fakeProcessName"
            }
        }
    }
})
