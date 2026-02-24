/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAllInAnyOrder
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.BPMN_PROCESS_DEFINITION_KEY
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import kotlin.jvm.optionals.getOrNull

class ZaaktypeBpmnConfigurationBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zaaktypeBpmnConfigurationCriteriaQuery = mockk<CriteriaQuery<ZaaktypeBpmnConfiguration>>()
    val zaaktypeBpmnConfigurationRoot = mockk<Root<ZaaktypeBpmnConfiguration>>()
    val path = mockk<Path<Any>>()
    val stringPath = mockk<Path<String>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()

    val zaaktypeBpmnConfigurationBeheerService = ZaaktypeBpmnConfigurationBeheerService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Multiple zaaktypeBpmnConfigurations with two unique BPMN process definition keys") {
        val uniqueBpmnProcessDefinitionKeys = listOf("fakeBpmnProcessDefinitionKey", "fakeBpmnProcessDefinitionKey2")
        val stringCriteriaQuery = mockk<CriteriaQuery<String>>()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(String::class.java) } returns stringCriteriaQuery
        every {
            stringCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
        } returns zaaktypeBpmnConfigurationRoot
        every { zaaktypeBpmnConfigurationRoot.get<String>(BPMN_PROCESS_DEFINITION_KEY) } returns stringPath
        every {
            stringCriteriaQuery.select(stringPath)
        } returns stringCriteriaQuery
        every { stringCriteriaQuery.distinct(true) } returns stringCriteriaQuery
        every {
            entityManager.createQuery(stringCriteriaQuery).resultList
        } returns uniqueBpmnProcessDefinitionKeys

        When("Returning the unique BPMN process definition keys") {
            val keys = zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeys()

            Then("Gives a list of two unique BPM process definition keys") {
                keys shouldContainAllInAnyOrder uniqueBpmnProcessDefinitionKeys
            }
        }
    }

    Given("One zaaktypeBpmnConfiguration for a given zaaktype omschrijving") {
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
        } returns zaaktypeBpmnConfigurationCriteriaQuery
        every {
            zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
        } returns zaaktypeBpmnConfigurationRoot
        every {
            zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
        } returns zaaktypeBpmnConfigurationCriteriaQuery
        every { zaaktypeBpmnConfigurationRoot.get<Any>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME) } returns path
        every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns path
        every { criteriaBuilder.equal(path, zaaktypeBpmnConfiguration.zaaktypeOmschrijving) } returns predicate
        every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
        every {
            entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst()
                .getOrNull()
        } returns zaaktypeBpmnConfiguration
        every { criteriaBuilder.desc(path) } returns order
        every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(order) } returns zaaktypeBpmnConfigurationCriteriaQuery

        When("Finding the configuration by zaaktype omschrijving") {
            val foundConfiguration =
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeBpmnConfiguration.zaaktypeOmschrijving)
            Then("The correct configuration is returned") {
                assert(foundConfiguration == zaaktypeBpmnConfiguration)
            }
        }
    }
})
