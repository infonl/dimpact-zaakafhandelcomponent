/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAKTYPE_UUID
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_IDENTIFICATIE
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createReferentieProcess
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.flowable.bpmn.exception.ProcessDefinitionNotFoundException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.runtime.ProcessInstance
import org.flowable.engine.runtime.ProcessInstanceBuilder
import java.net.URI
import java.util.UUID

class BpmnServiceTest : BehaviorSpec({
    val repositoryService = mockk<RepositoryService>()
    val runtimeService = mockk<RuntimeService>()
    val processEngine = mockk<ProcessEngine>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val bpmnService = BpmnService(
        repositoryService,
        runtimeService,
        processEngine,
        zaaktypeBpmnConfigurationBeheerService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A UUID for which a BPMN process instance exists") {
        val uuid = UUID.randomUUID()
        val processInstance = mockk<ProcessInstance>()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(uuid.toString())
                .singleResult()
        } returns processInstance

        When("a check is done to see if the zaak is process driven") {
            val isProcessDriven = bpmnService.isZaakProcessDriven(uuid)

            Then("'true is returned") {
                isProcessDriven shouldBe true
            }
        }
    }

    Given("A UUID for which no BPMN process instance exists") {
        val uuid = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(uuid.toString())
                .singleResult()
        } returns null

        When("a check is done to see if the zaak is process driven") {
            val isProcessDriven = bpmnService.isZaakProcessDriven(uuid)

            Then("'false is returned") {
                isProcessDriven shouldBe false
            }
        }
    }

    Given("A zaak and zaakdata and a zaaktype with a 'referentieproces'") {
        val referentieProcesName = "fakeReferentieProces"
        val zaakTypeUUID = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaakType = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"),
            referentieProces = createReferentieProcess(name = referentieProcesName)
        )
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            uuid = zaakUUID
        )
        val zaakData = mapOf<String, Any>("fakeKey" to "fakeValue")
        val processInstanceBuilder = mockk<ProcessInstanceBuilder>()
        val processInstance = mockk<ProcessInstance>()
        every {
            runtimeService.createProcessInstanceBuilder()
        } returns processInstanceBuilder
        every {
            processInstanceBuilder
                .processDefinitionKey(referentieProcesName)
                .businessKey(zaakUUID.toString())
                .variable(VAR_ZAAK_UUID, zaakUUID)
                .variable(VAR_ZAAK_IDENTIFICATIE, zaak.identificatie)
                .variable(VAR_ZAAKTYPE_UUID, zaakTypeUUID)
                .variable(VAR_ZAAKTYPE_OMSCHRIJVING, zaakType.omschrijving)
        } returns processInstanceBuilder
        every { processInstanceBuilder.variables(zaakData) } returns processInstanceBuilder
        every { processInstanceBuilder.start() } returns processInstance

        When("the zaak is started using a BPMN process definition") {
            bpmnService.startProcess(zaak, zaakType, referentieProcesName, zaakData)

            Then("a Flowable BPMN process instance should be started") {
                verify(exactly = 1) {
                    processInstanceBuilder.start()
                }
            }
        }
    }
    Given("A valid zaaktype UUID with a process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
        every {
            zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID)
        } returns zaaktypeBpmnProcessDefinition

        When("finding the process definition for the zaaktype") {
            val result = bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)

            Then("the correct process definition is returned") {
                result shouldBe zaaktypeBpmnProcessDefinition
            }
        }
    }

    Given("A valid zaaktype UUID without a process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        every { zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID) } returns null

        When("finding the process definition for the zaaktype") {
            val exception = shouldThrow<ProcessDefinitionNotFoundException> {
                bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)
            }

            Then("null is returned") {
                exception.message shouldContain "$zaaktypeUUID"
            }
        }
    }

    Given("A valid process definition key with an existing process definition") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        val processDefinition = createProcessDefinition()
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns processDefinition

        When("reading the process definition by process definition key") {
            val result = bpmnService.readProcessDefinitionByProcessDefinitionKey(processDefinitionKey)

            Then("the correct process definition is returned") {
                result shouldBe processDefinition
            }
        }
    }

    Given("An invalid process definition key with no existing process definition") {
        val processDefinitionKey = "fakeProcessDefinitionKey"
        every { bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey) } returns null

        When("reading the process definition by process definition key") {
            val exception = shouldThrow<ProcessDefinitionNotFoundException> {
                bpmnService.readProcessDefinitionByProcessDefinitionKey(processDefinitionKey)
            }

            Then("a 'process definition not found exception' is thrown") {
                exception.message shouldBe "No BPMN process definition found for process definition key: '$processDefinitionKey'"
            }
        }
    }

    Given("A valid zaaktype UUID with an existing process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        val processInstance = mockk<ProcessInstance>()
        val processInstanceId = "fakeProcessInstanceId"
        every {
            processInstance.id
        } returns processInstanceId
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaaktypeUUID.toString())
                .singleResult()
        } returns processInstance
        every {
            runtimeService.deleteProcessInstance(processInstanceId, null)
        } returns Unit

        When("Terminating the process instance by zaak UUID") {
            bpmnService.terminateCase(zaaktypeUUID)

            Then("the process instance is terminated") {
                verify(exactly = 1) {
                    runtimeService.deleteProcessInstance(processInstanceId, null)
                }
            }
        }
    }

    Given("valid zaaktype UUID without an existing process definition\"") {
        val zaaktypeUUID = UUID.randomUUID()
        every {
            runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaaktypeUUID.toString())
                .singleResult()
        } returns null

        When("Terminating the process instance by zaak UUID") {
            bpmnService.terminateCase(zaaktypeUUID)

            Then("the process instance is not found") {
                verify(exactly = 0) {
                    runtimeService.deleteProcessInstance(any(), null)
                }
            }
        }
    }
})
