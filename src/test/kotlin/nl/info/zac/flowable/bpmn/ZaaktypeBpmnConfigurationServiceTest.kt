/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.exception.ZaaktypeInUseException
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnConfiguration
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
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService =
        ZaaktypeBpmnConfigurationService(entityManager, zaaktypeCmmnConfigurationBeheerService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating a zaaktype - BPMN process definition") {
        Given("A zaaktype - BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(id = null)
            every {
                zaaktypeCmmnConfigurationBeheerService.readZaaktypeCmmnConfiguration(zaaktypeBpmnProcessDefinition.zaaktypeUuid)
            } returns null
            every { entityManager.persist(zaaktypeBpmnProcessDefinition) } just Runs
            every { entityManager.flush() } just Runs
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByZaaktypeUuid(
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )
            } returns zaaktypeBpmnProcessDefinition

            When("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationService.storeZaaktypeBpmnConfiguration(zaaktypeBpmnProcessDefinition)

                Then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.persist(zaaktypeBpmnProcessDefinition)
                        entityManager.flush()
                    }
                }
            }
        }

        Given("A zaaktype and existing CMMN mapping for it") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every {
                zaaktypeCmmnConfigurationBeheerService.readZaaktypeCmmnConfiguration(zaaktypeBpmnProcessDefinition.zaaktypeUuid)
            } returns createZaaktypeCmmnConfiguration()

            When("the zaaktype BPMN process definition relation is created") {
                val exception = shouldThrow<ZaaktypeInUseException> {
                    zaaktypeBpmnConfigurationService.storeZaaktypeBpmnConfiguration(
                        zaaktypeBpmnProcessDefinition
                    )
                }

                Then("an exception is thrown") {
                    exception.message shouldContain zaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                }
            }
        }
    }

    Context("Updating a zaaktype - BPMN process definition") {
        Given("An existing zaaktype - BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every {
                zaaktypeCmmnConfigurationBeheerService.readZaaktypeCmmnConfiguration(zaaktypeBpmnProcessDefinition.zaaktypeUuid)
            } returns null
            every { entityManager.merge(zaaktypeBpmnProcessDefinition) } returns zaaktypeBpmnProcessDefinition

            When("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationService.storeZaaktypeBpmnConfiguration(zaaktypeBpmnProcessDefinition)

                Then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.merge(zaaktypeBpmnProcessDefinition)
                    }
                }
            }
        }
    }

    Context("Deleting a zaaktype - BPMN process definition") {
        Given("A stored zaaktype BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every { entityManager.remove(zaaktypeBpmnProcessDefinition) } just Runs

            When("the zaaktype BPMN process definition relation is deleted") {
                zaaktypeBpmnConfigurationService.deleteZaaktypeBpmnConfiguration(zaaktypeBpmnProcessDefinition)

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
                    zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByZaaktypeUuid(zaaktypeUUID)

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
                    zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByZaaktypeUuid(zaaktypeUUID)

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
                    zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(
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
                    zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(
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
                val result = zaaktypeBpmnConfigurationService.listZaaktypeBpmnConfigurations()

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
                val result = zaaktypeBpmnConfigurationService.listZaaktypeBpmnConfigurations()

                Then("an empty list is returned") {
                    result shouldBe emptyList()
                }
            }
        }
    }
})
