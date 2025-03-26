/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
import net.atos.zac.flowable.bpmn.model.ZaaktypeBpmnProcessDefinition
import net.atos.zac.flowable.bpmn.model.createZaaktypeBpmnProcessDefinition
import java.util.Optional
import java.util.UUID

class ZaaktypeBpmnProcessDefinitionServiceTest : BehaviorSpec({
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<ZaaktypeBpmnProcessDefinition>>()
    val root = mockk<Root<ZaaktypeBpmnProcessDefinition>>()
    val predicate = mockk<Predicate>()
    val pathUuid = mockk<Path<UUID>>()
    val entityManager = mockk<EntityManager>()
    val zaaktypeBpmnProcessDefinitionService = ZaaktypeBpmnProcessDefinitionService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaaktype - BPMN process definition relation") {
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnProcessDefinition()
        every { entityManager.persist(zaaktypeBpmnProcessDefinition) } just Runs

        When("the zaaktype BPMN process definition relation is created") {
            zaaktypeBpmnProcessDefinitionService.createZaaktypeBpmnProcessDefinition(zaaktypeBpmnProcessDefinition)

            Then("the zaaktype BPMN process definition relation is persisted") {
                verify(exactly = 1) {
                    entityManager.persist(zaaktypeBpmnProcessDefinition)
                }
            }
        }
    }
    Given("A stored zaaktype BPMN process definition relation") {
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnProcessDefinition()
        every { entityManager.remove(zaaktypeBpmnProcessDefinition) } just Runs

        When("the zaaktype BPMN process definition relation is deleted") {
            zaaktypeBpmnProcessDefinitionService.deleteZaaktypeBpmnProcessDefinition(zaaktypeBpmnProcessDefinition)

            Then("the zaaktype BPMN process definition relation is removed") {
                verify(exactly = 1) {
                    entityManager.remove(zaaktypeBpmnProcessDefinition)
                }
            }
        }
    }
    Given("A valid zaaktype UUID with a corresponding BPMN process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnProcessDefinition(zaaktypeUuid = zaaktypeUUID)
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ZaaktypeBpmnProcessDefinition::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ZaaktypeBpmnProcessDefinition::class.java) } returns root
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { criteriaBuilder.equal(pathUuid, zaaktypeUUID) } returns predicate
        every { root.get<UUID>("zaaktypeUuid") } returns pathUuid
        every {
            entityManager.createQuery(criteriaQuery).resultStream.findFirst()
        } returns Optional.of(zaaktypeBpmnProcessDefinition)

        When("finding the BPMN process definition by zaaktype UUID") {
            val result = zaaktypeBpmnProcessDefinitionService.findZaaktypeProcessDefinitionByZaaktypeUuid(zaaktypeUUID)

            Then("the BPMN process definition is returned") {
                result shouldBe zaaktypeBpmnProcessDefinition
            }
        }
    }
    Given("A valid zaaktype UUID without a corresponding BPMN process definition") {
        val zaaktypeUUID = UUID.randomUUID()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ZaaktypeBpmnProcessDefinition::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ZaaktypeBpmnProcessDefinition::class.java) } returns root
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { criteriaBuilder.equal(pathUuid, zaaktypeUUID) } returns predicate
        every { root.get<UUID>("zaaktypeUuid") } returns pathUuid
        every {
            entityManager.createQuery(criteriaQuery).resultStream.findFirst()
        } returns Optional.empty()

        When("finding the BPMN process definition by zaaktype UUID") {
            val result = zaaktypeBpmnProcessDefinitionService.findZaaktypeProcessDefinitionByZaaktypeUuid(zaaktypeUUID)

            Then("null is returned") {
                result shouldBe null
            }
        }
    }
})
