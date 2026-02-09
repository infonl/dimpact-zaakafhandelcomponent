/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import java.util.UUID

class ZaaktypeBpmnConfigurationServiceTest : BehaviorSpec({
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService = ZaaktypeBpmnConfigurationService(zaaktypeBpmnConfigurationBeheerService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Checking if productaanvraagtype is in use for a change of a specific BPMN zaaktype") {
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()

        Given("No productaanvraagtype is in use by a BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!
                )
            } returns null

            When("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!,
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )

                Then("no exception is thrown") {}
            }
        }

        Given("A productaanvraagtype that is in use by the same BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!
                )
            } returns zaaktypeBpmnProcessDefinition

            When("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!,
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )

                Then("no exception is thrown") {}
            }
        }

        Given("A productaanvraagtype that is in use by another BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!
                )
            } returns createZaaktypeBpmnConfiguration(zaaktypeUuid = UUID.randomUUID())

            When("checking if productaanvraagtype is in use") {
                shouldThrow<InputValidationFailedException> {
                    zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                        zaaktypeBpmnProcessDefinition.productaanvraagtype!!,
                        zaaktypeBpmnProcessDefinition.zaaktypeUuid
                    )
                }

                Then("an exception is thrown") {}
            }
        }
    }

    Context("Checking if productaanvraagtype is in use in all BPMN zaaktypes") {
        val productaanvraagtype = "fakeProductaanvraagtypeUnderTest"

        Given("A productaanvraagtype is in use by a BPMN zaaktype") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(
                productaanvraagtype = productaanvraagtype
            )
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(productaanvraagtype)
            } returns zaaktypeBpmnProcessDefinition

            When("checking if productaanvraagtype is in use") {
                shouldThrow<InputValidationFailedException> {
                    zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
                }
                Then("an exception is thrown") {}
            }
        }

        Given("A productaanvraagtype is not use by a BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(productaanvraagtype)
            } returns null

            When("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
                Then("no exception is thrown") {}
            }
        }
    }
})
