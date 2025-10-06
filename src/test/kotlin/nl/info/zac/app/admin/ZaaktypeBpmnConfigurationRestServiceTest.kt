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
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService

class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()

    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val policyService = mockk<PolicyService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeBpmnConfigurationRestService =
        ZaaktypeBpmnConfigurationRestService(zaaktypeBpmnConfigurationService, policyService, ztcClientService)
    val zaakType = createZaakType()

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Listing BPMN zaaktypes") {
        Given("BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeBpmnConfigurationService.listBpmnProcessDefinitions()
            } returns listOf(zaaktypeBpmnProcessDefinition)
            every {
                ztcClientService.readZaaktype(zaaktypeBpmnProcessDefinition.zaaktypeUuid)
            } returns zaakType

            When("listing BPMN zaaktypes") {
                val result = zaaktypeBpmnConfigurationRestService.listZaaktypeBpmnProcessDefinition(
                    zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                )

                Then("it should return a list of BPMN zaaktypes") {
                    with(result) {
                        with(zaaktype) {
                            uuid shouldBe zaakType.url.extractUuid()
                            identificatie shouldBe zaakType.identificatie
                            doel shouldBe zaakType.doel
                            omschrijving shouldBe zaakType.omschrijving
                            servicenorm shouldBe zaakType.isServicenormAvailable()
                            versiedatum shouldBe zaakType.versiedatum
                            beginGeldigheid shouldBe zaakType.beginGeldigheid
                            eindeGeldigheid shouldBe zaakType.eindeGeldigheid
                            vertrouwelijkheidaanduiding shouldBe zaakType.vertrouwelijkheidaanduiding
                            nuGeldig shouldBe zaakType.isNuGeldig()
                        }
                        bpmnProcessDefinitionKey shouldBe zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                        productaanvraagtype shouldBe zaaktypeBpmnProcessDefinition.productaanvraagtype
                        groepNaam shouldBe zaaktypeBpmnProcessDefinition.groupId
                    }
                }
            }
        }

        Given("No BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeBpmnConfigurationService.listBpmnProcessDefinitions()
            } returns emptyList()

            When("listing BPMN zaaktypes") {
                val exception = shouldThrow<NotFoundException> {
                    zaaktypeBpmnConfigurationRestService.listZaaktypeBpmnProcessDefinition(
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
                zaaktypeBpmnConfigurationService.listBpmnProcessDefinitions()
            } returns listOf(zaaktypeBpmnProcessDefinition, zaaktypeBpmnProcessDefinition)

            When("listing BPMN zaaktypes") {
                val exception = shouldThrow<IllegalStateException> {
                    zaaktypeBpmnConfigurationRestService.listZaaktypeBpmnProcessDefinition(
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
