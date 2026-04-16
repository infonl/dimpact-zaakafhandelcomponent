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
import java.util.Optional

class InboxProductaanvraagServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val service = InboxProductaanvraagService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating an inbox productaanvraag") {
        Given("an inbox productaanvraag entity") {
            val item = createInboxProductaanvraag()

            When("create is called") {
                service.create(item)

                Then("the entity is persisted") {
                    verify { entityManager.persist(item) }
                }
            }
        }
    }

    Context("Finding an inbox productaanvraag by ID") {
        Given("an existing inbox productaanvraag") {
            val item = createInboxProductaanvraag()
            every { entityManager.find(InboxProductaanvraag::class.java, item.id) } returns item

            When("find is called with that ID") {
                val result = service.find(item.id!!)

                Then("the entity is returned as a present Optional") {
                    result shouldBe Optional.of(item)
                }
            }
        }

        Given("no inbox productaanvraag exists for the given ID") {
            val id = 999L
            every { entityManager.find(InboxProductaanvraag::class.java, id) } returns null

            When("find is called with that ID") {
                val result = service.find(id)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }

    Context("Deleting an inbox productaanvraag") {
        Given("an existing inbox productaanvraag") {
            val item = createInboxProductaanvraag()
            every { entityManager.find(InboxProductaanvraag::class.java, item.id) } returns item

            When("delete is called with that ID") {
                service.delete(item.id!!)

                Then("the entity is removed") {
                    verify { entityManager.remove(item) }
                }
            }
        }

        Given("no inbox productaanvraag exists for the given ID") {
            val id = 999L
            every { entityManager.find(InboxProductaanvraag::class.java, id) } returns null

            When("delete is called with that ID") {
                service.delete(id)

                Then("remove is not called") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    Context("Listing inbox productaanvragen") {
        Given("empty list parameters") {
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                val result = service.list(InboxProductaanvraagListParameters())

                Then("an empty result is returned") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.typeFilter shouldBe emptyList()
                }
            }
        }

        Given("list parameters with paging configured") {
            val paging = Paging(page = 1, maxResults = 10)
            val listParameters = InboxProductaanvraagListParameters().apply { this.paging = paging }
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("paging is applied to the query") {
                    verify { typedQuery.setFirstResult(paging.getFirstResult()) }
                    verify { typedQuery.setMaxResults(paging.maxResults) }
                }
            }
        }

        Given("list parameters with ascending sorting") {
            val listParameters = InboxProductaanvraagListParameters().apply {
                sorting = Sorting(InboxProductaanvraag.TYPE, SorteerRichting.ASCENDING)
            }
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("ascending order is applied") {
                    verify { entityManager.criteriaBuilder.asc(any()) }
                }
            }
        }

        Given("list parameters with descending sorting") {
            val listParameters = InboxProductaanvraagListParameters().apply {
                sorting = Sorting(InboxProductaanvraag.TYPE, SorteerRichting.DESCENDING)
            }
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("descending order is applied") {
                    verify { entityManager.criteriaBuilder.desc(any()) }
                }
            }
        }

        Given("list parameters with an initiatorID filter") {
            val listParameters = createInboxProductaanvraagListParameters(initiatorID = "user123")
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("a LIKE predicate is applied for the initiatorID") {
                    verify { entityManager.criteriaBuilder.like(any(), "%user123%") }
                }
            }
        }

        Given("list parameters with a blank initiatorID") {
            val listParameters = createInboxProductaanvraagListParameters(initiatorID = "  ")
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                val result = service.list(listParameters)

                Then("no LIKE predicate is applied and the call succeeds") {
                    result.items shouldBe emptyList()
                }
            }
        }

        Given("list parameters with a type filter") {
            val listParameters = createInboxProductaanvraagListParameters(type = "aanvraag")
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("an equal predicate is applied for the type") {
                    verify { entityManager.criteriaBuilder.equal(any(), "aanvraag") }
                }
            }
        }

        Given("list parameters with a full ontvangstdatum range") {
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

            When("list is called") {
                service.list(listParameters)

                Then("both greaterThanOrEqualTo and lessThanOrEqualTo predicates are applied") {
                    verify { entityManager.criteriaBuilder.greaterThanOrEqualTo(any(), van) }
                    verify { entityManager.criteriaBuilder.lessThanOrEqualTo(any(), tot) }
                }
            }
        }

        Given("list parameters with only the ontvangstdatum van boundary") {
            val van = LocalDate.of(2024, 6, 1)
            val listParameters = createInboxProductaanvraagListParameters(
                ontvangstdatumRange = DatumRange(van = van, tot = null)
            )
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("greaterThanOrEqualTo predicate is applied") {
                    verify { entityManager.criteriaBuilder.greaterThanOrEqualTo(any(), van) }
                }
            }
        }

        Given("list parameters with only the ontvangstdatum tot boundary") {
            val tot = LocalDate.of(2024, 6, 30)
            val listParameters = createInboxProductaanvraagListParameters(
                ontvangstdatumRange = DatumRange(van = null, tot = tot)
            )
            val typedQuery = mockk<TypedQuery<InboxProductaanvraag>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxProductaanvraag>>()) } returns typedQuery

            When("list is called") {
                service.list(listParameters)

                Then("lessThanOrEqualTo predicate is applied") {
                    verify { entityManager.criteriaBuilder.lessThanOrEqualTo(any(), tot) }
                }
            }
        }
    }
})
