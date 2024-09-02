/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.admin.model.createZaakafhandelParameters
import java.time.ZonedDateTime
import java.util.Date

class ZaakafhandelParameterBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zaakafhandelparametersCriteriaQuery = mockk<CriteriaQuery<ZaakafhandelParameters>>()
    val dateSubquery = mockk<Subquery<Date>>()
    val zaakafhandelparametersTypedQuery = mockk<TypedQuery<ZaakafhandelParameters>>()
    val zaakafhandelparametersRoot = mockk<Root<ZaakafhandelParameters>>()
    val path = mockk<Path<Any>>()
    val pathString = mockk<Path<String>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
    val expressionString = mockk<Expression<String>>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()

    val zaakafhandelParameterBeheerService = ZaakafhandelParameterBeheerService(
        entityManager = entityManager,
        ztcClientService = ztcClientService,
        zaakafhandelParameterService = zaakafhandelParameterService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("One zaakafhandelparameters for a given zaaktype UUID") {
        val zaakafhandelparameters = createZaakafhandelParameters()
        val now = ZonedDateTime.now()
        every { ztcClientService.resetCacheTimeToNow() } returns now
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersCriteriaQuery
        every {
            zaakafhandelparametersCriteriaQuery.from(ZaakafhandelParameters::class.java)
        } returns zaakafhandelparametersRoot
        every {
            zaakafhandelparametersCriteriaQuery.select(zaakafhandelparametersRoot)
        } returns zaakafhandelparametersCriteriaQuery
        every { zaakafhandelparametersRoot.get<Any>("zaakTypeUUID") } returns path
        every { criteriaBuilder.equal(path, zaakafhandelparameters.zaakTypeUUID) } returns predicate
        every { zaakafhandelparametersCriteriaQuery.where(predicate) } returns zaakafhandelparametersCriteriaQuery
        every {
            entityManager.createQuery(zaakafhandelparametersCriteriaQuery).resultList
        } returns listOf(zaakafhandelparameters)

        When("the zaakafhandelparameters are retrieved based on the zaaktypeUUID") {
            val returnedZaakafhandelParameters = zaakafhandelParameterBeheerService.readZaakafhandelParameters(
                zaakafhandelparameters.zaakTypeUUID
            )

            Then("the zaakafhandelparameters should be returned") {
                with(returnedZaakafhandelParameters) {
                    zaakTypeUUID shouldBe zaakafhandelparameters.zaakTypeUUID
                }
            }
        }
    }
    Given("Two zaakafhandelparameters") {
        val zaakafhandelparameters = listOf(
            createZaakafhandelParameters(),
            createZaakafhandelParameters()
        )
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersCriteriaQuery
        every {
            zaakafhandelparametersCriteriaQuery.from(ZaakafhandelParameters::class.java)
        } returns zaakafhandelparametersRoot
        every {
            zaakafhandelparametersCriteriaQuery.select(zaakafhandelparametersRoot)
        } returns zaakafhandelparametersCriteriaQuery
        every { entityManager.createQuery(zaakafhandelparametersCriteriaQuery).resultList } returns zaakafhandelparameters
        every { zaakafhandelparametersRoot.get<Any>("id") } returns path
        every { criteriaBuilder.desc(path) } returns order
        every { zaakafhandelparametersCriteriaQuery.orderBy(order) } returns zaakafhandelparametersCriteriaQuery

        When("the zaakafhandelparameters are retrieved based on the zaaktypeUUID") {
            val returnedZaakafhandelParameters = zaakafhandelParameterBeheerService.listZaakafhandelParameters()

            Then("both zaakafhandelparameters should be returned") {
                returnedZaakafhandelParameters.size shouldBe 2
                returnedZaakafhandelParameters.forEachIndexed { index, returnedZaakafhandelparameter ->
                    with(returnedZaakafhandelparameter) {
                        zaakTypeUUID shouldBe zaakafhandelparameters[index].zaakTypeUUID
                        id shouldBe zaakafhandelparameters[index].id
                    }
                }
            }
        }
    }
    Given(
        """
        Two active zaakafhandelparameters for a given productaanvraagType
        """
    ) {
        val productaanvraagType = "dummyProductaanvraagType"
        val zaakafhandelparametersList = listOf(
            createZaakafhandelParameters(),
            createZaakafhandelParameters()
        )
        with(entityManager) {
            every { createQuery(zaakafhandelparametersCriteriaQuery) } returns zaakafhandelparametersTypedQuery
            every { getCriteriaBuilder() } returns criteriaBuilder
        }
        with(criteriaBuilder) {
            every { createQuery(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersCriteriaQuery
            every { greatest<String>(any()) } returns expressionString
            every { equal(pathString, pathString) } returns predicate
            every { equal(pathString, productaanvraagType) } returns predicate
            every { equal(pathString, dateSubquery) } returns predicate
        }
        every { criteriaBuilder.and(predicate, predicate) } returns predicate
        with(zaakafhandelparametersCriteriaQuery) {
            every { from(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersRoot
            every { subquery(Date::class.java) } returns dateSubquery
            every { select(zaakafhandelparametersRoot) } returns zaakafhandelparametersCriteriaQuery
            every { where(predicate) } returns zaakafhandelparametersCriteriaQuery
        }
        with(dateSubquery) {
            every { from(ZaakafhandelParameters::class.java) } returns mockk {
                every { get<String>("creatiedatum") } returns pathString
                every { get<String>("zaaktypeOmschrijving") } returns pathString
            }
            every { select(any()) } returns dateSubquery
            every { where(predicate) } returns dateSubquery
        }
        with(zaakafhandelparametersRoot) {
            every { get<String>("zaaktypeOmschrijving") } returns pathString
            every { zaakafhandelparametersRoot.get<String>("productaanvraagtype") } returns pathString
            every { zaakafhandelparametersRoot.get<String>("creatiedatum") } returns pathString
        }
        every { zaakafhandelparametersTypedQuery.resultList } returns zaakafhandelparametersList

        When(
            """
                the active zaakafhandelparameters are retrieved for the given productaanvraagType
                """
        ) {
            val returnedZaakafhandelParameters = zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(
                productaanvraagType
            )

            Then("two zaakafhandelparameters should be returned") {
                returnedZaakafhandelParameters.size shouldBe 2
                returnedZaakafhandelParameters.map { productaanvraagType } shouldContainOnly listOf(productaanvraagType)
            }
        }
    }
})
