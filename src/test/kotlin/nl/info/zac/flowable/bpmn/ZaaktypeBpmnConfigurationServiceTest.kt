/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import java.util.Optional
import java.util.UUID

class ZaaktypeBpmnConfigurationServiceTest : BehaviorSpec({
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<ZaaktypeBpmnConfiguration>>()
    val root = mockk<Root<ZaaktypeBpmnConfiguration>>()
    val predicate = mockk<Predicate>()
    val pathUuid = mockk<Path<UUID>>()
    val pathProductAanvraagType = mockk<Path<String>>()
    val entityManager = mockk<EntityManager>()
    val zaaktypeBpmnConfigurationService = ZaaktypeBpmnConfigurationService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Checking if productaanvraagtype is in use for a change of a specific BPMN zaaktype") {
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()

        Given("No productaanvraagtype is in use by a BPMN zaaktype") {
            every {
                zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(zaaktypeBpmnProcessDefinition.productaanvraagtype!!)
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
                zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(zaaktypeBpmnProcessDefinition.productaanvraagtype!!)
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
                zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(zaaktypeBpmnProcessDefinition.productaanvraagtype!!)
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
                zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(productaanvraagtype)
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
                zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(productaanvraagtype)
            } returns null

            When("checking if productaanvraagtype is in use") {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
                Then("no exception is thrown") {}
            }
        }
    }

    Context("Creating a zaaktype - BPMN process definition (no id)") {
        Given("A zaaktype - BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(id = null)
            every { entityManager.persist(zaaktypeBpmnProcessDefinition) } just Runs
            every { entityManager.flush() } just Runs
            every {
                zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )
            } returns zaaktypeBpmnProcessDefinition

            When("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationService.storeConfiguration(zaaktypeBpmnProcessDefinition)

                Then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.persist(zaaktypeBpmnProcessDefinition)
                        entityManager.flush()
                    }
                }
            }
        }
    }

    Context("Updating a zaaktype - BPMN process definition (existing id)") {
        Given("An existing zaaktype - BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every {
                zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )
            } returns zaaktypeBpmnProcessDefinition
            every { entityManager.merge(zaaktypeBpmnProcessDefinition) } returns zaaktypeBpmnProcessDefinition

            When("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationService.storeConfiguration(zaaktypeBpmnProcessDefinition)

                Then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.merge(zaaktypeBpmnProcessDefinition)
                    }
                }
            }
        }

        Given("A 'pristine' zaaktype - BPMN process definition relation") {
            val cmmnId = 1L
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(id = cmmnId)
            every {
                zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )
            } returns null andThen zaaktypeBpmnProcessDefinition
            val zaaktypeBpmnConfigurationSlot = slot<ZaaktypeBpmnConfiguration>()
            every { entityManager.persist(capture(zaaktypeBpmnConfigurationSlot)) } just Runs
            every { entityManager.flush() } just Runs

            When("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationService.storeConfiguration(zaaktypeBpmnProcessDefinition)

                Then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.persist(zaaktypeBpmnProcessDefinition)
                        entityManager.flush()
                    }
                }

                And("the ID was reset") {
                    zaaktypeBpmnConfigurationSlot.captured.id shouldBe null
                }
            }
        }
    }

    Context("Deleting a zaaktype - BPMN process definition") {
        Given("A stored zaaktype BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every { entityManager.remove(zaaktypeBpmnProcessDefinition) } just Runs

            When("the zaaktype BPMN process definition relation is deleted") {
                zaaktypeBpmnConfigurationService.deleteConfiguration(zaaktypeBpmnProcessDefinition)

                Then("the zaaktype BPMN process definition relation is removed") {
                    verify(exactly = 1) {
                        entityManager.remove(zaaktypeBpmnProcessDefinition)
                    }
                }
            }
        }
    }

    Context("Finding a BPMN process definition by zaaktype UUID") {
        Given("A valid zaaktype UUID with a corresponding BPMN process definition") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(zaaktypeUuid = zaaktypeUUID)
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java) } returns criteriaQuery
            every { criteriaQuery.from(ZaaktypeBpmnConfiguration::class.java) } returns root
            every { criteriaQuery.where(predicate) } returns criteriaQuery
            every { criteriaBuilder.equal(pathUuid, zaaktypeUUID) } returns predicate
            every { root.get<UUID>("zaaktypeUuid") } returns pathUuid
            every {
                entityManager.createQuery(criteriaQuery).resultStream.findFirst()
            } returns Optional.of(zaaktypeBpmnProcessDefinition)

            When("finding the BPMN process definition by zaaktype UUID") {
                val result =
                    zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(zaaktypeUUID)

                Then("the BPMN process definition is returned") {
                    result shouldBe zaaktypeBpmnProcessDefinition
                }
            }
        }
        Given("A valid zaaktype UUID without a corresponding BPMN process definition") {
            val zaaktypeUUID = UUID.randomUUID()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java) } returns criteriaQuery
            every { criteriaQuery.from(ZaaktypeBpmnConfiguration::class.java) } returns root
            every { criteriaQuery.where(predicate) } returns criteriaQuery
            every { criteriaBuilder.equal(pathUuid, zaaktypeUUID) } returns predicate
            every { root.get<UUID>("zaaktypeUuid") } returns pathUuid
            every {
                entityManager.createQuery(criteriaQuery).resultStream.findFirst()
            } returns Optional.empty()

            When("finding the BPMN process definition by zaaktype UUID") {
                val result =
                    zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(zaaktypeUUID)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Finding a BPMN process definition by productaanvraagtype") {
        Given("A productaanvraagtype with a corresponding BPMN process definition") {
            val productAanvraagType = "fakeProductaanvraagtype"
            val definition = createZaaktypeBpmnConfiguration()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java) } returns criteriaQuery
            every { criteriaQuery.from(ZaaktypeBpmnConfiguration::class.java) } returns root
            every { criteriaQuery.where(predicate) } returns criteriaQuery
            every { root.get<String>("productaanvraagtype") } returns pathProductAanvraagType
            every { criteriaBuilder.equal(pathProductAanvraagType, productAanvraagType) } returns predicate
            every { entityManager.createQuery(criteriaQuery).resultStream.findFirst() } returns Optional.of(definition)

            When("finding the BPMN process definition by productaanvraagtype") {
                val result =
                    zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(
                        productAanvraagType
                    )

                Then("the BPMN process definition is returned") {
                    result shouldBe definition
                }
            }
        }

        Given("A productaanvraagtype without a corresponding BPMN process definition") {
            val productAanvraagType = "notExistingProductaanvraagtype"
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java) } returns criteriaQuery
            every { criteriaQuery.from(ZaaktypeBpmnConfiguration::class.java) } returns root
            every { criteriaQuery.where(predicate) } returns criteriaQuery
            every { root.get<String>("productaanvraagtype") } returns pathProductAanvraagType
            every { criteriaBuilder.equal(pathProductAanvraagType, productAanvraagType) } returns predicate
            every { entityManager.createQuery(criteriaQuery).resultStream.findFirst() } returns Optional.empty()

            When("finding the BPMN process definition by productaanvraagtype") {
                val result =
                    zaaktypeBpmnConfigurationService.findConfigurationByProductAanvraagType(
                        productAanvraagType
                    )

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("listing all BPMN definitions") {
        Given("Listing all BPMN process definitions when definitions exist") {
            val definition1 = createZaaktypeBpmnConfiguration()
            val definition2 = createZaaktypeBpmnConfiguration()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java) } returns criteriaQuery
            every { criteriaQuery.from(ZaaktypeBpmnConfiguration::class.java) } returns root
            every { entityManager.createQuery(criteriaQuery).resultList } returns listOf(definition1, definition2)

            When("listing BPMN process definitions") {
                val result = zaaktypeBpmnConfigurationService.listConfigurations()

                Then("a list with all BPMN process definitions is returned") {
                    result shouldBe listOf(definition1, definition2)
                }
            }
        }

        Given("Listing all BPMN process definitions when none exist") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every { criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java) } returns criteriaQuery
            every { criteriaQuery.from(ZaaktypeBpmnConfiguration::class.java) } returns root
            every { entityManager.createQuery(criteriaQuery).resultList } returns emptyList()

            When("listing BPMN process definitions") {
                val result = zaaktypeBpmnConfigurationService.listConfigurations()

                Then("an empty list is returned") {
                    result shouldBe emptyList()
                }
            }
        }
    }
})
