/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import nl.info.zac.productaanvraag.model.InboxProductaanvraag
import nl.info.zac.productaanvraag.model.InboxProductaanvraagListParameters
import nl.info.zac.productaanvraag.model.createInboxProductaanvraag
import nl.info.zac.productaanvraag.model.createInboxProductaanvraagListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.Paging
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.shared.model.Sorting
import java.time.LocalDate

class InboxProductaanvraagServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val service = InboxProductaanvraagService(entityManager)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating an inbox productaanvraag") {
        given("an inbox productaanvraag entity") {
            val item = createInboxProductaanvraag()

            `when`("create is called") {
                service.create(item)

                then("the entity is persisted") {
                    verify { entityManager.persist(item) }
                }
            }
        }
    }

    context("Finding an inbox productaanvraag by ID") {
        given("an existing inbox productaanvraag") {
            val inboxProductaanvraag = createInboxProductaanvraag()
            every { entityManager.find(InboxProductaanvraag::class.java, inboxProductaanvraag.id) } returns inboxProductaanvraag

            `when`("find is called with that ID") {
                val result = service.find(inboxProductaanvraag.id!!)

                then("the entity is returned") {
                    result shouldBe inboxProductaanvraag
                }
            }
        }

        given("no inbox productaanvraag exists for the given ID") {
            val id = 999L
            every { entityManager.find(InboxProductaanvraag::class.java, id) } returns null

            `when`("find is called with that ID") {
                val result = service.find(id)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("Deleting an inbox productaanvraag") {
        given("an existing inbox productaanvraag") {
            val item = createInboxProductaanvraag()
            every { entityManager.find(InboxProductaanvraag::class.java, item.id) } returns item

            `when`("delete is called with that ID") {
                service.delete(item.id!!)

                then("the entity is removed") {
                    verify { entityManager.remove(item) }
                }
            }
        }

        given("no inbox productaanvraag exists for the given ID") {
            val id = 999L
            every { entityManager.find(InboxProductaanvraag::class.java, id) } returns null

            `when`("delete is called with that ID") {
                service.delete(id)

                then("remove is not called") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    context("Listing inbox productaanvragen") {
        given("empty list parameters") {
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                val result = service.list(InboxProductaanvraagListParameters())

                then("an empty result is returned") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.typeFilter shouldBe emptyList()
                }
            }
        }

        given("list parameters with paging configured") {
            val paging = Paging(page = 1, maxResults = 10)
            val listParameters = InboxProductaanvraagListParameters().apply { this.paging = paging }
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("paging is applied to the query") {
                    verify { typedQuery.setFirstResult(paging.getFirstResult()) }
                    verify { typedQuery.setMaxResults(paging.maxResults) }
                }
            }
        }

        given("list parameters with ascending sorting") {
            val listParameters = InboxProductaanvraagListParameters().apply {
                sorting = Sorting(InboxProductaanvraag.TYPE, SorteerRichting.ASCENDING)
            }
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("ascending order is applied") {
                    verify { entityManager.criteriaBuilder.asc(any()) }
                }
            }
        }

        given("list parameters with descending sorting") {
            val listParameters = InboxProductaanvraagListParameters().apply {
                sorting = Sorting(InboxProductaanvraag.TYPE, SorteerRichting.DESCENDING)
            }
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("descending order is applied") {
                    verify { entityManager.criteriaBuilder.desc(any()) }
                }
            }
        }

        given("list parameters with an initiatorID filter") {
            val listParameters = createInboxProductaanvraagListParameters(initiatorID = "user123")
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("a LIKE predicate is applied for the initiatorID") {
                    verify { entityManager.criteriaBuilder.like(any(), "%user123%") }
                }
            }
        }

        given("list parameters with a blank initiatorID") {
            val listParameters = createInboxProductaanvraagListParameters(initiatorID = "  ")
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                val result = service.list(listParameters)

                then("no LIKE predicate is applied and the call succeeds") {
                    result.items shouldBe emptyList()
                }
            }
        }

        given("list parameters with a type filter") {
            val listParameters = createInboxProductaanvraagListParameters(type = "aanvraag")
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("an equal predicate is applied for the type") {
                    verify { entityManager.criteriaBuilder.equal(any(), "aanvraag") }
                }
            }
        }

        given("list parameters with a full ontvangstdatum range") {
            val van = LocalDate.of(2024, 1, 1)
            val tot = LocalDate.of(2024, 12, 31)
            val listParameters = createInboxProductaanvraagListParameters(
                ontvangstdatumRange = DatumRange(van = van, tot = tot)
            )
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("both greaterThanOrEqualTo and lessThanOrEqualTo predicates are applied") {
                    verify { entityManager.criteriaBuilder.greaterThanOrEqualTo(any(), van) }
                    verify { entityManager.criteriaBuilder.lessThanOrEqualTo(any(), tot) }
                }
            }
        }

        given("list parameters with only the ontvangstdatum van boundary") {
            val van = LocalDate.of(2024, 6, 1)
            val listParameters = createInboxProductaanvraagListParameters(
                ontvangstdatumRange = DatumRange(van = van, tot = null)
            )
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("greaterThanOrEqualTo predicate is applied") {
                    verify { entityManager.criteriaBuilder.greaterThanOrEqualTo(any(), van) }
                }
            }
        }

        given("list parameters with only the ontvangstdatum tot boundary") {
            val tot = LocalDate.of(2024, 6, 30)
            val listParameters = createInboxProductaanvraagListParameters(
                ontvangstdatumRange = DatumRange(van = null, tot = tot)
            )
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            `when`("list is called") {
                service.list(listParameters)

                then("lessThanOrEqualTo predicate is applied") {
                    verify { entityManager.criteriaBuilder.lessThanOrEqualTo(any(), tot) }
                }
            }
        }
    }
})
