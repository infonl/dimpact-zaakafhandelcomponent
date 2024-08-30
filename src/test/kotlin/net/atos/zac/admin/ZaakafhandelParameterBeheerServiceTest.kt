/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.admin.model.createZaakafhandelParameters
import java.time.ZonedDateTime

class ZaakafhandelParameterBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<ZaakafhandelParameters>>()
    val root = mockk<Root<ZaakafhandelParameters>>()
    val path = mockk<Path<Any>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
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
        every { criteriaBuilder.createQuery(ZaakafhandelParameters::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ZaakafhandelParameters::class.java) } returns root
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { root.get<Any>("zaakTypeUUID") } returns path
        every { criteriaBuilder.equal(path, zaakafhandelparameters.zaakTypeUUID) } returns predicate
        every { criteriaQuery.where(predicate) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery).resultList } returns listOf(zaakafhandelparameters)

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
        every { criteriaBuilder.createQuery(ZaakafhandelParameters::class.java) } returns criteriaQuery
        every { criteriaQuery.from(ZaakafhandelParameters::class.java) } returns root
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery).resultList } returns zaakafhandelparameters
        every { root.get<Any>("id") } returns path
        every { criteriaBuilder.desc(path) } returns order
        every { criteriaQuery.orderBy(order) } returns criteriaQuery

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
})
