/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import nl.info.zac.admin.MailTemplateKoppelingenService
import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.admin.model.createMailtemplateKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration

class MailTemplateKoppelingenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<ZaaktypeCmmnMailtemplateParameters>>()
    val root = mockk<Root<ZaaktypeCmmnMailtemplateParameters>>()
    val typedQuery = mockk<TypedQuery<ZaaktypeCmmnMailtemplateParameters>>()
    val service = MailTemplateKoppelingenService(entityManager)

    afterEach {
        checkUnnecessaryStub()
    }

    Given("A mail template koppeling exists") {
        val id = 42L
        val koppeling = createMailtemplateKoppelingen(
            id = id,
            zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.find(ZaaktypeCmmnMailtemplateParameters::class.java, id) } returns koppeling

        When("find is called with the id") {
            val result = service.find(id)

            Then("the same koppeling instance is returned") {
                (result === koppeling) shouldBe true
            }
        }

        When("delete is called with the id") {
            every { entityManager.remove(any<ZaaktypeCmmnMailtemplateParameters>()) } just runs

            service.delete(id)

            Then("entityManager.remove is called with the entity") {
                verify { entityManager.remove(any<ZaaktypeCmmnMailtemplateParameters>()) }
            }
        }
    }

    Given("No mail template koppeling exists for the given id") {
        val id = 99L
        every { entityManager.find(ZaaktypeCmmnMailtemplateParameters::class.java, id) } returns null

        When("find is called with the id") {
            val result = service.find(id)

            Then("null is returned") {
                result shouldBe null
            }
        }

When("readMailtemplateKoppeling is called with the id") {
    val exception = shouldThrow<NoSuchElementException> {
        service.readMailtemplateKoppeling(id)
    }

    Then("a NoSuchElementException is thrown containing the class name and id") {
        exception.message!!.let {
            it.contains(ZaaktypeCmmnMailtemplateParameters::class.java.simpleName) shouldBe true
            it.contains(id.toString()) shouldBe true
        }
    }
}
    }

    Given("storeMailtemplateKoppeling with a new entity (no id)") {
        val koppeling = createMailtemplateKoppelingen(
            id = null,
            zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.persist(any<ZaaktypeCmmnMailtemplateParameters>()) } just runs

        When("storeMailtemplateKoppeling is called") {
            val result = service.storeMailtemplateKoppeling(koppeling)

            Then("entityManager.persist is called and the same entity instance is returned") {
                verify { entityManager.persist(any<ZaaktypeCmmnMailtemplateParameters>()) }
                (result === koppeling) shouldBe true
            }
        }
    }

    Given("storeMailtemplateKoppeling with an existing entity (id present)") {
        val id = 7L
        val koppeling = createMailtemplateKoppelingen(
            id = id,
            zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        val merged = createMailtemplateKoppelingen(
            id = id,
            zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.find(ZaaktypeCmmnMailtemplateParameters::class.java, id) } returns koppeling
        every { entityManager.merge(any<ZaaktypeCmmnMailtemplateParameters>()) } returns merged

        When("storeMailtemplateKoppeling is called") {
            val result = service.storeMailtemplateKoppeling(koppeling)

            Then("entityManager.merge is called and the merged entity instance is returned") {
                verify { entityManager.merge(any<ZaaktypeCmmnMailtemplateParameters>()) }
                (result === merged) shouldBe true
            }
        }
    }

    Given("An existing mail template koppeling") {
        val id = 5L
        val koppeling = createMailtemplateKoppelingen(
            id = id,
            zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.find(ZaaktypeCmmnMailtemplateParameters::class.java, id) } returns koppeling

        When("readMailtemplateKoppeling is called with the id") {
            val result = service.readMailtemplateKoppeling(id)

            Then("the same koppeling instance is returned") {
                (result === koppeling) shouldBe true
            }
        }
    }

    Given("Two mail template koppelingen in the database") {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaaktypeCmmnMailtemplateParameters::class.java)
        } returns criteriaQuery
        every { criteriaQuery.from(ZaaktypeCmmnMailtemplateParameters::class.java) } returns root
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        val koppelingen = listOf(
            createMailtemplateKoppelingen(
                zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
                mailTemplate = createMailTemplate()
            ),
            createMailtemplateKoppelingen(
                zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
                mailTemplate = createMailTemplate()
            )
        )
        every { typedQuery.resultList } returns koppelingen

        When("listMailtemplateKoppelingen is called") {
            val result = service.listMailtemplateKoppelingen()

            Then("both koppelingen are returned") {
                result.size shouldBe 2
            }
        }
    }
})
