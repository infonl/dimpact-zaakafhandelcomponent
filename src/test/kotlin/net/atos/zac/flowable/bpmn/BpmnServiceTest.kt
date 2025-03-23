/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_UUID
import net.atos.zac.flowable.bpmn.model.createZaaktypeBpmnProcessDefinition
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createReferentieProcess
import nl.info.client.zgw.ztc.model.createZaakType
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
    val bpmnProcessDefinitionService = mockk<BpmnProcessDefinitionService>()
    val bpmnService = BpmnService(
        repositoryService,
        runtimeService,
        processEngine,
        bpmnProcessDefinitionService
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
            val isProcessDriven = bpmnService.isProcessDriven(uuid)

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
            val isProcessDriven = bpmnService.isProcessDriven(uuid)

            Then("'false is returned") {
                isProcessDriven shouldBe false
            }
        }
    }

    Given("A zaak and zaakdata and a zaaktype with a 'referentieproces'") {
        val referentieProcesName = "dummyReferentieProces"
        val zaakTypeUUID = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaakType = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"),
            referentieProces = createReferentieProcess(name = referentieProcesName)
        )
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            uuid = zaakUUID
        )
        val zaakData = mapOf<String, Any>("dummyKey" to "dummyValue")
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
                .variable(VAR_ZAAKTYPE_UUUID, zaakTypeUUID)
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
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnProcessDefinition()
        every { bpmnProcessDefinitionService.findZaaktypeProcessDefinition(zaaktypeUUID) } returns zaaktypeBpmnProcessDefinition

        When("finding the process definition for the zaaktype") {
            val result = bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)

            Then("the correct process definition is returned") {
                result shouldBe zaaktypeBpmnProcessDefinition
            }
        }
    }

    Given("A valid zaaktype UUID without a process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        every { bpmnProcessDefinitionService.findZaaktypeProcessDefinition(zaaktypeUUID) } returns null

        When("finding the process definition for the zaaktype") {
            val result = bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)

            Then("null is returned") {
                result shouldBe null
            }
        }
    }
})
