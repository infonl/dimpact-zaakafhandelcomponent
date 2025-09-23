/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.NotFoundException
import nl.info.zac.flowable.bpmn.ZaaktypeBpmnProcessDefinitionService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnProcessDefinition
import nl.info.zac.policy.PolicyService

class ZaaktypeBpmnProcessDefinitionRestServiceTest : BehaviorSpec({
    val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnProcessDefinition()

    val zaaktypeBpmnProcessDefinitionService = mockk<ZaaktypeBpmnProcessDefinitionService>()
    val policyService = mockk<PolicyService>()
    val zaaktypeBpmnProcessDefinitionRestService =
        ZaaktypeBpmnProcessDefinitionRestService(zaaktypeBpmnProcessDefinitionService, policyService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Listing BPMN zaaktypes") {
        Given("BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeBpmnProcessDefinitionService.listBpmnProcessDefinitions()
            } returns listOf(zaaktypeBpmnProcessDefinition)

            When("listing BPMN zaaktypes") {
                val result = zaaktypeBpmnProcessDefinitionRestService.listZaaktypeBpmnProcessDefinition(
                    zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                )

                Then("it should return a list of BPMN zaaktypes") {
                    with(result) {
                        zaaktypeUuid shouldBe zaaktypeBpmnProcessDefinition.zaaktypeUuid
                        zaaktypeOmschrijving shouldBe zaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                        bpmnProcessDefinitionKey shouldBe zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                        productaanvraagtype shouldBe zaaktypeBpmnProcessDefinition.productaanvraagtype
                        groepNaam shouldBe zaaktypeBpmnProcessDefinition.groepNaam
                    }
                }
            }
        }

        Given("No BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeBpmnProcessDefinitionService.listBpmnProcessDefinitions()
            } returns emptyList()

            When("listing BPMN zaaktypes") {
                val exception = shouldThrow<NotFoundException> {
                    zaaktypeBpmnProcessDefinitionRestService.listZaaktypeBpmnProcessDefinition(
                        zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                    )
                }

                Then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                }
            }
        }

        Given("Multiple zaaktypes mapped to one process definition") {
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeBpmnProcessDefinitionService.listBpmnProcessDefinitions()
            } returns listOf(zaaktypeBpmnProcessDefinition, zaaktypeBpmnProcessDefinition)

            When("listing BPMN zaaktypes") {
                val exception = shouldThrow<IllegalStateException> {
                    zaaktypeBpmnProcessDefinitionRestService.listZaaktypeBpmnProcessDefinition(
                        zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                    )
                }

                Then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                }
            }
        }
    }
})
