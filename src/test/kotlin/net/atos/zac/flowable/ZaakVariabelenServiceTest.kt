/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.flowable.cmmn.api.CmmnHistoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.runtime.CaseInstance
import org.flowable.engine.HistoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.runtime.ProcessInstance
import java.util.UUID

class ZaakVariabelenServiceTest : BehaviorSpec({
    val cmmnRuntimeService = mockk<CmmnRuntimeService>()
    val cmmnHistoryService = mockk<CmmnHistoryService>()
    val bpmnRuntimeService = mockk<RuntimeService>()
    val bpmnHistoryService = mockk<HistoryService>()
    val zaakVariabelenService = ZaakVariabelenService(
        cmmnRuntimeService,
        cmmnHistoryService,
        bpmnRuntimeService,
        bpmnHistoryService
    )
    val zaakUUID = UUID.randomUUID()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A case instance with a CMMN process that has zaak UUID case variable") {
        val planItemInstance = createTestPlanItemInstance()
        val caseInstanceId = planItemInstance.getCaseInstanceId()
        val caseInstance = mockk<CaseInstance>()
        val caseVariables = mapOf("zaakUUID" to zaakUUID)
        every { caseInstance.caseVariables } returns caseVariables

        When("the zaak UUID variable is read") {
            every {
                cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .includeCaseVariables()
                    .singleResult()
            } returns caseInstance

            val returnedZaakUUID = zaakVariabelenService.readZaakUUID(planItemInstance)

            Then("the zaak UUID is correctly returned") {
                returnedZaakUUID shouldBe zaakUUID
                verify(exactly = 1) {
                    cmmnRuntimeService.createCaseInstanceQuery()
                }
            }
        }

        When("the zaak data is changed") {
            val variables = mapOf("test1" to 1, "test2" to 2)
            every {
                cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns caseInstance
            every { caseInstance.id } returns caseInstanceId
            every { cmmnRuntimeService.setVariables(caseInstanceId, variables) } just runs

            zaakVariabelenService.setZaakdata(zaakUUID, variables)

            Then("the correct call to CMMN service is executed") {
                verify(exactly = 1) {
                    cmmnRuntimeService.setVariables(caseInstanceId, variables)
                }
            }
        }

        When("expected suspend days is set") {
            every {
                cmmnRuntimeService.createCaseInstanceQuery()
                    .variableValueEquals("zaakUUID", zaakUUID)
                    .singleResult()
            } returns caseInstance
            every { cmmnRuntimeService.setVariable(caseInstanceId, "verwachteDagenOpgeschort", 1) } just runs

            zaakVariabelenService.setVerwachteDagenOpgeschort(zaakUUID, 1)

            Then("the correct call to BPMN service is executed") {
                verify(exactly = 1) {
                    cmmnRuntimeService.setVariable(caseInstanceId, "verwachteDagenOpgeschort", 1)
                }
            }
        }

        When("removing suspend days") {
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .caseInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns caseInstance
            every { cmmnRuntimeService.removeVariable(caseInstanceId, "verwachteDagenOpgeschort") } just runs

            zaakVariabelenService.removeVerwachteDagenOpgeschort(zaakUUID)

            Then("the correct call to BPMN service is executed") {
                verify(exactly = 1) {
                    cmmnRuntimeService.removeVariable(caseInstanceId, "verwachteDagenOpgeschort")
                }
            }
        }
    }

    Given("A case instance with a running BPMN process") {
        val processInstance = mockk<ProcessInstance>()
        val processVariables = mapOf(
            "a" to 1,
            "b" to 2
        )

        When("reading zaak process data") {
            every {
                bpmnRuntimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(zaakUUID.toString())
                    .includeProcessVariables().singleResult()
            } returns processInstance
            every { processInstance.processVariables } returns processVariables

            val variablesMap = zaakVariabelenService.readProcessZaakdata(zaakUUID)

            Then("correct data is returned") {
                variablesMap shouldContainExactly processVariables
            }
        }

        When("zaak data is set") {
            val processInstanceId = "id"
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .caseInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns null
            every {
                bpmnRuntimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns processInstance
            every { processInstance.id } returns processInstanceId
            every { bpmnRuntimeService.setVariables(processInstanceId, processVariables) } just runs

            zaakVariabelenService.setZaakdata(zaakUUID, processVariables)

            Then("the correct call to BPMN service is executed") {
                verify(exactly = 1) {
                    bpmnRuntimeService.setVariables(processInstanceId, processVariables)
                }
            }
        }

        When("expected suspend days is set") {
            val processInstanceId = "id"
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .variableValueEquals("zaakUUID", zaakUUID)
                    .singleResult()
            } returns null
            every {
                bpmnRuntimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns processInstance
            every { processInstance.id } returns processInstanceId
            every { bpmnRuntimeService.setVariable(processInstanceId, "verwachteDagenOpgeschort", 1) } just runs

            zaakVariabelenService.setVerwachteDagenOpgeschort(zaakUUID, 1)

            Then("the correct call to BPMN service is executed") {
                verify(exactly = 1) {
                    bpmnRuntimeService.setVariable(processInstanceId, "verwachteDagenOpgeschort", 1)
                }
            }
        }

        When("removing suspend days") {
            val processInstanceId = "id"
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .caseInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns null
            every {
                bpmnRuntimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns processInstance
            every { processInstance.id } returns processInstanceId
            every { bpmnRuntimeService.removeVariable(processInstanceId, "verwachteDagenOpgeschort") } just runs

            zaakVariabelenService.removeVerwachteDagenOpgeschort(zaakUUID)

            Then("the correct call to BPMN service is executed") {
                verify(exactly = 1) {
                    bpmnRuntimeService.removeVariable(processInstanceId, "verwachteDagenOpgeschort")
                }
            }
        }
    }

    Given("A case instance with a historic BPMN process") {
        val processVariables = mapOf(
            "a" to 1,
            "b" to 2
        )
        every {
            bpmnRuntimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .includeProcessVariables()
                .singleResult()
        } returns null
        every {
            bpmnHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .includeProcessVariables()
                .singleResult()
                .processVariables
        } returns processVariables

        When("reading zaak process data") {
            val variablesMap = zaakVariabelenService.readProcessZaakdata(zaakUUID)

            Then("correct data is returned") {
                variablesMap shouldContainExactly processVariables
            }
        }
    }

    Given("A case instance without an active or historic BPMN process") {
        every {
            bpmnRuntimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .includeProcessVariables()
                .singleResult()
        } returns null
        every {
            bpmnHistoryService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .includeProcessVariables()
                .singleResult()
        } returns null

        When("reading zaak process data") {
            val variablesMap = zaakVariabelenService.readProcessZaakdata(zaakUUID)

            Then("empty map is returned") {
                variablesMap shouldContainExactly emptyMap()
            }
        }
    }

    Given("A case instance without a known process type") {
        every {
            bpmnRuntimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult()
        } returns null

        When("zaak data is set") {
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .caseInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns null

            val exception = shouldThrow<RuntimeException> {
                zaakVariabelenService.setZaakdata(zaakUUID, mapOf("a" to 1))
            }

            Then("error message should contain zaak UUID") {
                exception.message shouldContain zaakUUID.toString()
            }
        }

        When("expected suspend days is set") {
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .variableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
                    .singleResult()
            } returns null

            val exception = shouldThrow<RuntimeException> {
                zaakVariabelenService.setVerwachteDagenOpgeschort(zaakUUID, 1)
            }

            Then("error message should contain zaak UUID") {
                exception.message shouldContain zaakUUID.toString()
            }
        }

        When("removing suspend days") {
            every {
                cmmnRuntimeService
                    .createCaseInstanceQuery()
                    .caseInstanceBusinessKey(zaakUUID.toString())
                    .singleResult()
            } returns null

            zaakVariabelenService.removeVerwachteDagenOpgeschort(zaakUUID)

            Then("no exception should be thrown") {}
        }
    }
})
