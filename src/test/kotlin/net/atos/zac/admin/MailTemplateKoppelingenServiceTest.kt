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
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaaktypeMailtemplateParameters
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.admin.model.createMailtemplateKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration

class MailTemplateKoppelingenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<ZaaktypeMailtemplateParameters>>()
    val root = mockk<Root<ZaaktypeMailtemplateParameters>>()
    val typedQuery = mockk<TypedQuery<ZaaktypeMailtemplateParameters>>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val service = MailTemplateKoppelingenService(entityManager, zaaktypeConfigurationService)

    afterEach {
        checkUnnecessaryStub()
    }

    given("A mail template koppeling exists") {
        val id = 42L
        val koppeling = createMailtemplateKoppelingen(
            id = id,
            zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.find(ZaaktypeMailtemplateParameters::class.java, id) } returns koppeling

        `when`("find is called with the id") {
            val result = service.find(id)

            then("the same koppeling instance is returned") {
                (result === koppeling) shouldBe true
            }
        }

        `when`("delete is called with the id") {
            every { entityManager.remove(any<ZaaktypeMailtemplateParameters>()) } just runs
            every { zaaktypeConfigurationService.evict(koppeling.zaaktypeConfiguration.zaaktypeUuid) } just runs

            service.delete(id)

            then(
                """
                the koppeling is removed and its zaaktype configuration is evicted from the cache,
                so that a cached configuration does not keep serving the deleted koppeling
                """
            ) {
                verify {
                    entityManager.remove(koppeling)
                    zaaktypeConfigurationService.evict(koppeling.zaaktypeConfiguration.zaaktypeUuid)
                }
            }
        }
    }

    given("No mail template koppeling exists for the given id") {
        val id = 99L
        every { entityManager.find(ZaaktypeMailtemplateParameters::class.java, id) } returns null

        `when`("find is called with the id") {
            val result = service.find(id)

            then("null is returned") {
                result shouldBe null
            }
        }

        `when`("readMailtemplateKoppeling is called with the id") {
            val exception = shouldThrow<NoSuchElementException> {
                service.readMailtemplateKoppeling(id)
            }

            then("a NoSuchElementException is thrown containing the class name and id") {
                exception.message!!.let {
                    it.contains(ZaaktypeMailtemplateParameters::class.java.simpleName) shouldBe true
                    it.contains(id.toString()) shouldBe true
                }
            }
        }
    }

    given("storeMailtemplateKoppeling with a new entity (no id)") {
        val koppeling = createMailtemplateKoppelingen(
            id = null,
            zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.persist(any<ZaaktypeMailtemplateParameters>()) } just runs
        every { zaaktypeConfigurationService.evict(koppeling.zaaktypeConfiguration.zaaktypeUuid) } just runs

        `when`("storeMailtemplateKoppeling is called") {
            val result = service.storeMailtemplateKoppeling(koppeling)

            then(
                """
                entityManager.persist is called, the same entity instance is returned
                and its zaaktype configuration is evicted from the cache
                """
            ) {
                verify {
                    entityManager.persist(any<ZaaktypeMailtemplateParameters>())
                    zaaktypeConfigurationService.evict(koppeling.zaaktypeConfiguration.zaaktypeUuid)
                }
                (result === koppeling) shouldBe true
            }
        }
    }

    given("storeMailtemplateKoppeling with an existing entity (id present)") {
        val id = 7L
        val koppeling = createMailtemplateKoppelingen(
            id = id,
            zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        val merged = createMailtemplateKoppelingen(
            id = id,
            zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.find(ZaaktypeMailtemplateParameters::class.java, id) } returns koppeling
        every { entityManager.merge(any<ZaaktypeMailtemplateParameters>()) } returns merged
        every { zaaktypeConfigurationService.evict(merged.zaaktypeConfiguration.zaaktypeUuid) } just runs

        `when`("storeMailtemplateKoppeling is called") {
            val result = service.storeMailtemplateKoppeling(koppeling)

            then(
                """
                entityManager.merge is called, the merged entity instance is returned
                and its zaaktype configuration is evicted from the cache
                """
            ) {
                verify {
                    entityManager.merge(any<ZaaktypeMailtemplateParameters>())
                    zaaktypeConfigurationService.evict(merged.zaaktypeConfiguration.zaaktypeUuid)
                }
                (result === merged) shouldBe true
            }
        }
    }

    given("An existing mail template koppeling") {
        val id = 5L
        val koppeling = createMailtemplateKoppelingen(
            id = id,
            zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
            mailTemplate = createMailTemplate()
        )
        every { entityManager.find(ZaaktypeMailtemplateParameters::class.java, id) } returns koppeling

        `when`("readMailtemplateKoppeling is called with the id") {
            val result = service.readMailtemplateKoppeling(id)

            then("the same koppeling instance is returned") {
                (result === koppeling) shouldBe true
            }
        }
    }

    given("Two mail template koppelingen in the database") {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaaktypeMailtemplateParameters::class.java)
        } returns criteriaQuery
        every { criteriaQuery.from(ZaaktypeMailtemplateParameters::class.java) } returns root
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        val koppelingen = listOf(
            createMailtemplateKoppelingen(
                zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
                mailTemplate = createMailTemplate()
            ),
            createMailtemplateKoppelingen(
                zaaktypeConfiguration = createZaaktypeCmmnConfiguration(),
                mailTemplate = createMailTemplate()
            )
        )
        every { typedQuery.resultList } returns koppelingen

        `when`("listMailtemplateKoppelingen is called") {
            val result = service.listMailtemplateKoppelingen()

            then("both koppelingen are returned") {
                result.size shouldBe 2
            }
        }
    }
})
