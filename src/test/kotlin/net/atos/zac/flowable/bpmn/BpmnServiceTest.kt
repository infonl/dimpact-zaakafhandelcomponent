/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.model.createReferentieProcess
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_UUID
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
    val bpmnService = BpmnService(
        repositoryService,
        runtimeService,
        processEngine
    )

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
        every { processInstance.id } returns "dummyProcessInstanceID"
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
                .variables(zaakData)
        } returns processInstanceBuilder
        every { processInstanceBuilder.start() } returns processInstance

        When("the zaak is started using a BPMN process definition") {
            bpmnService.startProcess(zaak, zaakType, zaakData)

            Then("a Flowable BPMN process instance should be started") {
                verify(exactly = 1) {
                    processInstanceBuilder.start()
                }
            }
        }
    }

    Given("A zaak and zaakdata and a zaaktype without a 'referentieproces'") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"))
        val zaak = createZaak(zaakTypeURI = zaakType.url)
        val zaakData = mapOf<String, Any>("dummyKey" to "dummyValue")

        When("the zaak is started using a BPMN process definition") {
            val exception = shouldThrow<IllegalArgumentException> {
                bpmnService.startProcess(zaak, zaakType, zaakData)
            }

            Then("an exception should be thrown") {
                exception.message shouldBe "No referentieproces found for zaaktype with UUID: '$zaakTypeUUID'"
            }
        }
    }
})
