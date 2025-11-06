/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import java.util.UUID

class ZaaktypeBpmnConfigurationServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService =
        ZaaktypeBpmnConfigurationService(zaaktypeBpmnConfigurationBeheerService, ztcClientService)

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
                    zaaktypeBpmnProcessDefinition
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
                    zaaktypeBpmnProcessDefinition
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
                        zaaktypeBpmnProcessDefinition
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

    Context("copying configuration on a new zaaktype version") {

        Given("no previous configuration") {
            val zaakType = createZaakType()
            every { ztcClientService.readZaaktype(zaakType.url) } returns zaakType

            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaakType.omschrijving)
            } returns null

            When("copying configuration") {
                zaaktypeBpmnConfigurationService.copyConfiguration(zaakType.url)

                Then("no copying is done") {
                    verify(exactly = 0) {
                        entityManager.persist(any())
                        entityManager.merge(any())
                    }
                }
            }
        }

        Given("existing previous configuration") {
            val zaakType = createZaakType()
            val newZaaktypeUuid = zaakType.url.extractUuid()
            every { ztcClientService.readZaaktype(zaakType.url) } returns zaakType

            val previousConfiguration = createZaaktypeBpmnConfiguration()
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaakType.omschrijving)
            } returns previousConfiguration

            val configurationSlot = slot<ZaaktypeBpmnConfiguration>()
            val newConfiguration = createZaaktypeBpmnConfiguration()
            every {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(capture(configurationSlot))
            } returns newConfiguration

            When("copying configuration") {
                zaaktypeBpmnConfigurationService.copyConfiguration(zaakType.url)

                Then("correct copy is stored") {
                    with(configurationSlot.captured) {
                        zaaktypeUuid shouldBe newZaaktypeUuid
                    }
                }
            }
        }
    }
})
