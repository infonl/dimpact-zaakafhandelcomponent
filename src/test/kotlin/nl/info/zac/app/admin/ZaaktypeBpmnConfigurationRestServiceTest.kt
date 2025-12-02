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
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService

class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()

    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val policyService = mockk<PolicyService>()
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationRestService =
        ZaaktypeBpmnConfigurationRestService(
            zaaktypeBpmnConfigurationService,
            zaaktypeBpmnConfigurationBeheerService,
            zaaktypeCmmnConfigurationBeheerService,
            policyService
        )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Reading BPMN zaaktypes") {
        Given("BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns listOf(zaaktypeBpmnProcessDefinition)

            When("reading BPMN zaaktypes") {
                val result = zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                    zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                )

                Then("it should return a list of BPMN zaaktypes") {
                    with(result) {
                        id shouldBe zaaktypeBpmnProcessDefinition.id
                        zaaktypeUuid shouldBe zaaktypeBpmnProcessDefinition.zaaktypeUuid
                        zaaktypeOmschrijving shouldBe zaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                        bpmnProcessDefinitionKey shouldBe zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                        productaanvraagtype shouldBe zaaktypeBpmnProcessDefinition.productaanvraagtype
                        groepNaam shouldBe zaaktypeBpmnProcessDefinition.groepID
                    }
                }
            }
        }

        Given("No BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns emptyList()

            When("reading BPMN zaaktypes") {
                val exception = shouldThrow<NotFoundException> {
                    zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                        zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                    )
                }

                Then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                }
            }
        }

        Given("Multiple zaaktypes mapped to one process definition") {
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns listOf(zaaktypeBpmnProcessDefinition, zaaktypeBpmnProcessDefinition)

            When("reading BPMN zaaktypes") {
                val exception = shouldThrow<MultipleZaaktypeConfigurationsFoundException> {
                    zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
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
