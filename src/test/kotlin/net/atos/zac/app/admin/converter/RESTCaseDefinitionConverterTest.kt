/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.cmmn.CMMNService
import nl.info.zac.app.planitems.model.PlanItemType
import org.flowable.cmmn.api.repository.CaseDefinition
import org.flowable.cmmn.model.HumanTask
import org.flowable.cmmn.model.UserEventListener

class RESTCaseDefinitionConverterTest : BehaviorSpec({
    val cmmnService = mockk<CMMNService>()
    val converter = RESTCaseDefinitionConverter(cmmnService)

    afterEach { checkUnnecessaryStub() }

    Context("Convert CaseDefinition without relations") {
        Given("A CaseDefinition with name and key") {
            val caseDefinition = mockk<CaseDefinition> {
                every { name } returns "fakeCaseName"
                every { key } returns "fakeCaseKey"
            }

            When("convertToRESTCaseDefinition is called with inclusiefRelaties = false") {
                val result = converter.convertToRESTCaseDefinition(caseDefinition, false)

                Then("RESTCaseDefinition has correct name and key") {
                    result.naam shouldBe "fakeCaseName"
                    result.key shouldBe "fakeCaseKey"
                }

                And("humanTaskDefinitions and userEventListenerDefinitions are null") {
                    result.humanTaskDefinitions.shouldBeNull()
                    result.userEventListenerDefinitions.shouldBeNull()
                }
            }
        }
    }

    Context("Convert CaseDefinition with relations") {
        Given("A CaseDefinition with human tasks and user event listeners") {
            val fakeCaseDefinitionId = "fakeCaseDefinitionId"
            val caseDefinition = mockk<CaseDefinition> {
                every { name } returns "fakeCaseName"
                every { key } returns "fakeCaseKey"
                every { id } returns fakeCaseDefinitionId
            }
            val humanTask = mockk<HumanTask> {
                every { id } returns "fakeHumanTaskId"
                every { name } returns "fakeHumanTaskName"
            }
            val userEventListener = mockk<UserEventListener> {
                every { id } returns "fakeUserEventListenerId"
                every { name } returns "fakeUserEventListenerName"
            }
            every { cmmnService.listHumanTasks(fakeCaseDefinitionId) } returns listOf(humanTask)
            every { cmmnService.listUserEventListeners(fakeCaseDefinitionId) } returns listOf(userEventListener)

            When("convertToRESTCaseDefinition is called with inclusiefRelaties = true") {
                val result = converter.convertToRESTCaseDefinition(caseDefinition, true)

                Then("humanTaskDefinitions contains one entry with type HUMAN_TASK") {
                    result.humanTaskDefinitions!!.size shouldBe 1
                    result.humanTaskDefinitions!![0].id shouldBe "fakeHumanTaskId"
                    result.humanTaskDefinitions!![0].naam shouldBe "fakeHumanTaskName"
                    result.humanTaskDefinitions!![0].type shouldBe PlanItemType.HUMAN_TASK
                }

                And("userEventListenerDefinitions contains one entry with type USER_EVENT_LISTENER") {
                    result.userEventListenerDefinitions!!.size shouldBe 1
                    result.userEventListenerDefinitions!![0].id shouldBe "fakeUserEventListenerId"
                    result.userEventListenerDefinitions!![0].type shouldBe PlanItemType.USER_EVENT_LISTENER
                }
            }
        }
    }

    Context("Convert by case definition key") {
        Given("A case definition key") {
            val fakeCaseDefinitionKey = "fakeCaseDefinitionKey"
            val caseDefinition = mockk<CaseDefinition> {
                every { name } returns "fakeCaseName"
                every { key } returns fakeCaseDefinitionKey
            }
            every { cmmnService.readCaseDefinition(fakeCaseDefinitionKey) } returns caseDefinition

            When("convertToRESTCaseDefinition is called with a key string") {
                val result = converter.convertToRESTCaseDefinition(fakeCaseDefinitionKey, false)

                Then("CMMNService.readCaseDefinition is called and the result is converted") {
                    result.key shouldBe fakeCaseDefinitionKey
                }
            }
        }
    }
})
