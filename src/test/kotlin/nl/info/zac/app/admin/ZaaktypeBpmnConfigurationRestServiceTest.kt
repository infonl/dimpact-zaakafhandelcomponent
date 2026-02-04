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
import net.atos.zac.app.admin.converter.RESTZaakbeeindigParameterConverter
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.app.admin.model.createRestZaaktypeBpmnConfiguration
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService
import java.util.UUID

class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val policyService = mockk<PolicyService>()
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakbeeindigParameterConverter = mockk<RESTZaakbeeindigParameterConverter>()
    val zaaktypeBpmnConfigurationRestService =
        ZaaktypeBpmnConfigurationRestService(
            zaaktypeBpmnConfigurationService,
            zaaktypeBpmnConfigurationBeheerService,
            zaaktypeCmmnConfigurationBeheerService,
            policyService,
            ztcClientService,
            zaakbeeindigParameterConverter,
        )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Reading BPMN zaaktypes") {
        Given("BPMN zaaktype process definition is set-up") {
            val resultaatType = createResultaatType()
            val restResultType = resultaatType.toRestResultaatType()
            val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns listOf(zaaktypeBpmnProcessDefinition)
            every {
                zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any())
            } returns listOf(restZaakbeeindigParameter)
            every { ztcClientService.readResultaattype(any<UUID>()) } returns createResultaatType()

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

    Context("Creating or updating BPMN zaaktypes") {
        Given("A valid REST zaaktype BPMN configuration for a new zaaktype") {
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                groepNaam = "testGroep",
                productaanvraagtype = "testProductaanvraag"
            )
            val savedConfiguration = createZaaktypeBpmnConfiguration()

            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(any<UUID>())
            } returns null
            every {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(any())
            } returns savedConfiguration
            every { ztcClientService.readResultaattype(any<UUID>()) } returns createResultaatType()
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()

            When("creating a new zaaktype BPMN configuration") {
                val result = zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                Then("it should return the created configuration") {
                    result.zaaktypeUuid shouldBe savedConfiguration.zaaktypeUuid
                    result.bpmnProcessDefinitionKey shouldBe savedConfiguration.bpmnProcessDefinitionKey
                }
            }
        }

        Given("A valid REST zaaktype BPMN configuration for an existing zaaktype") {
            val existingConfiguration = createZaaktypeBpmnConfiguration()
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                id = existingConfiguration.id!!,
                zaaktypeUuid = existingConfiguration.zaaktypeUuid,
                groepNaam = "updatedGroep",
                productaanvraagtype = "updatedProductaanvraag"
            )
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(restZaaktypeBpmnConfiguration.zaaktypeUuid)
            } returns existingConfiguration
            every {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(any())
            } returns existingConfiguration
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()

            When("updating an existing zaaktype BPMN configuration") {
                val result = zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                Then("it should return the updated configuration") {
                    result.zaaktypeUuid shouldBe existingConfiguration.zaaktypeUuid
                }
            }
        }

        Given("A REST zaaktype BPMN configuration without a group name") {
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                groepNaam = null
            )

            every { policyService.readOverigeRechten().beheren } returns true

            When("creating a zaaktype BPMN configuration") {
                val exception = shouldThrow<IllegalStateException> {
                    zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                        restZaaktypeBpmnConfiguration
                    )
                }

                Then("it should throw an exception") {
                    exception.message shouldContain "groepNaam must not be null"
                }
            }
        }
    }
})
