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
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.exception.InputValidationFailedException
import java.util.UUID

class ZaaktypeBpmnConfigurationServiceTest : BehaviorSpec({
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService = ZaaktypeBpmnConfigurationService(zaaktypeBpmnConfigurationBeheerService)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Checking if productaanvraagtype is in use for a change of a specific BPMN zaaktype") {
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(
            productaanvraagtype = "fakeProductaanvraagtype"
        )

        given("No productaanvraagtype is in use by a BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!
                )
            } returns null

            `when`("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!,
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )

                then("no exception is thrown") {}
            }
        }

        given("A productaanvraagtype that is in use by the same BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!
                )
            } returns zaaktypeBpmnProcessDefinition

            `when`("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!,
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )

                then("no exception is thrown") {}
            }
        }

        given("A productaanvraagtype that is in use by another BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    zaaktypeBpmnProcessDefinition.productaanvraagtype!!
                )
            } returns createZaaktypeBpmnConfiguration(
                zaaktypeUUID = UUID.randomUUID(),
                productaanvraagtype = "fakeProductaanvraagtype"
            )

            `when`("checking if productaanvraagtype is in use") {
                shouldThrow<InputValidationFailedException> {
                    zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                        zaaktypeBpmnProcessDefinition.productaanvraagtype!!,
                        zaaktypeBpmnProcessDefinition.zaaktypeUuid
                    )
                }

                then("an exception is thrown") {}
            }
        }
    }

    context("Checking if productaanvraagtype is in use in all BPMN zaaktypes") {
        val productaanvraagtype = "fakeProductaanvraagtypeUnderTest"

        given("A productaanvraagtype is in use by a BPMN zaaktype") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(
                productaanvraagtype = productaanvraagtype
            )
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(productaanvraagtype)
            } returns zaaktypeBpmnProcessDefinition

            `when`("checking if productaanvraagtype is in use") {
                shouldThrow<InputValidationFailedException> {
                    zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
                }
                then("an exception is thrown") {}
            }
        }

        given("A productaanvraagtype is not use by a BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(productaanvraagtype)
            } returns null

            `when`("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
                then("no exception is thrown") {}
            }
        }
    }
})
