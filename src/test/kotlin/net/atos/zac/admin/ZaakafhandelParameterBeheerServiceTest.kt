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
import java.util.UUID

class ZaakafhandelParameterBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zaakafhandelparametersCriteriaQuery = mockk<CriteriaQuery<ZaakafhandelParameters>>()
    val zaakafhandelparametersRoot = mockk<Root<ZaakafhandelParameters>>()
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
    Given("A zaakafhandelparameters in the database for a specific zaaktype UUID and a productaanvraagType") {
        val productaanvraagType = "dummyProductaanvraagType"
        val zaaktypeUUID = UUID.randomUUID()
        val uuidCriteriaQuery = mockk<CriteriaQuery<UUID>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(UUID::class.java) } returns uuidCriteriaQuery
        every { uuidCriteriaQuery.from(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersRoot
        every { uuidCriteriaQuery.select(zaakafhandelparametersRoot.get("zaakTypeUUID")) } returns uuidCriteriaQuery
        every { zaakafhandelparametersRoot.get<Any>("productaanvraagtype") } returns path
        every { criteriaBuilder.equal(path, productaanvraagType) } returns predicate
        every { uuidCriteriaQuery.where(predicate) } returns uuidCriteriaQuery
        every { zaakafhandelparametersRoot.get<Any>("creatiedatum") } returns path
        every { criteriaBuilder.desc(path) } returns order
        every { uuidCriteriaQuery.orderBy(order) } returns uuidCriteriaQuery
        every { entityManager.createQuery(uuidCriteriaQuery).resultList } returns listOf(zaaktypeUUID)

        When(
            """
                the active zaaktype UUID is retrieved based on the productaanvraagType
                """
        ) {
            val returnedZaaktypeUUID = zaakafhandelParameterBeheerService.findActiveZaaktypeUuidByProductaanvraagType(
                productaanvraagType
            )

            Then("the zaaktype UUID for the given zaakafhandelparameters should be returned") {
                returnedZaaktypeUUID shouldBe zaaktypeUUID
            }
        }
    }
    Given(
        """
        Two zaakafhandelparameters for a specific zaaktype UUID and a productaanvraagType
        """
    ) {
        val productaanvraagType = "dummyProductaanvraagType"
        val zaaktypeUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
        val uuidCriteriaQuery = mockk<CriteriaQuery<UUID>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(UUID::class.java) } returns uuidCriteriaQuery
        every { uuidCriteriaQuery.from(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersRoot
        every { uuidCriteriaQuery.select(zaakafhandelparametersRoot.get("zaakTypeUUID")) } returns uuidCriteriaQuery
        every { zaakafhandelparametersRoot.get<Any>("productaanvraagtype") } returns path
        every { criteriaBuilder.equal(path, productaanvraagType) } returns predicate
        every { uuidCriteriaQuery.where(predicate) } returns uuidCriteriaQuery
        every { zaakafhandelparametersRoot.get<Any>("creatiedatum") } returns path
        every { criteriaBuilder.desc(path) } returns order
        every { uuidCriteriaQuery.orderBy(order) } returns uuidCriteriaQuery
        every { entityManager.createQuery(uuidCriteriaQuery).resultList } returns zaaktypeUUIDs

        When(
            """
                the active zaaktype UUID is retrieved from the set of zaakafhandelparameters based on the productaanvraagType
                """
        ) {
            val returnedZaaktypeUUID = zaakafhandelParameterBeheerService.findActiveZaaktypeUuidByProductaanvraagType(
                productaanvraagType
            )

            Then("the first zaaktype UUID for the given zaakafhandelparameters should be returned") {
                returnedZaaktypeUUID shouldBe zaaktypeUUIDs.first()
            }
        }
    }
//    Given(
//        """
//        Two zaakafhandelparameters with different zaaktype UUIDs but both with the same productaanvraagType
//        """
//    ) {
//        val productaanvraagType = "dummyProductaanvraagType"
//        val zaaktypeUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
//        val uuidCriteriaQuery = mockk<CriteriaQuery<UUID>>()
//
//        every { entityManager.criteriaBuilder } returns criteriaBuilder
//        every { criteriaBuilder.createQuery(UUID::class.java) } returns uuidCriteriaQuery
//        every { uuidCriteriaQuery.from(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersRoot
//        every { uuidCriteriaQuery.select(zaakafhandelparametersRoot.get("zaakTypeUUID")) } returns uuidCriteriaQuery
//        every { zaakafhandelparametersRoot.get<Any>("productaanvraagtype") } returns path
//        every { criteriaBuilder.equal(path, productaanvraagType) } returns predicate
//        every { uuidCriteriaQuery.where(predicate) } returns uuidCriteriaQuery
//        every { zaakafhandelparametersRoot.get<Any>("creatiedatum") } returns path
//        every { criteriaBuilder.desc(path) } returns order
//        every { uuidCriteriaQuery.orderBy(order) } returns uuidCriteriaQuery
//        every { entityManager.createQuery(uuidCriteriaQuery).resultList } returns zaaktypeUUIDs
//
//        When(
//            """
//                the active zaaktype UUID is retrieved from the set of zaakafhandelparameters based on the productaanvraagType
//                """
//        ) {
//            val returnedZaaktypeUUID = zaakafhandelParameterBeheerService.findActiveZaaktypeUuidByProductaanvraagType(
//                productaanvraagType
//            )
//
//            Then("the first zaaktype UUID for the given zaakafhandelparameters should be returned") {
//                returnedZaaktypeUUID shouldBe zaaktypeUUIDs.first()
//            }
//        }
//    }
})
