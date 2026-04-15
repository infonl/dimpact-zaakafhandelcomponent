/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.repository

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import nl.info.zac.document.inboxdocument.repository.model.InboxDocument
import nl.info.zac.document.inboxdocument.repository.model.InboxDocumentListParameters
import nl.info.zac.document.inboxdocument.repository.model.createInboxDocument
import nl.info.zac.document.inboxdocument.repository.model.createInboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.Paging
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.shared.model.Sorting
import java.time.LocalDate
import java.util.UUID

class InboxDocumentRepositoryTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val inboxDocumentRepository = InboxDocumentRepository(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Saving an inbox document") {
        Given("an inbox document entity") {
            val document = createInboxDocument()

            When("save is called") {
                inboxDocumentRepository.save(document)

                Then("the entity is persisted") {
                    verify { entityManager.persist(document) }
                }
            }
        }
    }

    Context("Finding an inbox document by ID") {
        Given("an existing inbox document with a known ID") {
            val document = createInboxDocument()
            every { entityManager.find(InboxDocument::class.java, document.id) } returns document

            When("find is called with that ID") {
                val result = inboxDocumentRepository.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no document exists for a given ID") {
            val id = 999L
            every { entityManager.find(InboxDocument::class.java, id) } returns null

            When("find is called with that ID") {
                val result = inboxDocumentRepository.find(id)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Finding an inbox document by UUID") {
        Given("an existing inbox document with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>> {
                every { resultList } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentRepository.find(document.enkelvoudiginformatieobjectUUID)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<InboxDocument>> {
                every { resultList } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentRepository.find(unknownUuid)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Deleting an inbox document") {
        Given("an existing inbox document") {
            val document = createInboxDocument()

            When("delete is called for that document") {
                inboxDocumentRepository.delete(document)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }
    }

    Context("Listing inbox documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(InboxDocumentListParameters())

                Then("an empty list is returned") {
                    result shouldBe emptyList()
                }
            }
        }

        Given("inbox documents exist") {
            val documents = listOf(createInboxDocument(id = 1L), createInboxDocument(id = 2L))
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called with empty parameters") {
                val result = inboxDocumentRepository.list(InboxDocumentListParameters())

                Then("all matching documents are returned") {
                    result shouldBe documents
                }
            }
        }

        Given("list parameters with paging configured") {
            val document = createInboxDocument()
            val paging = Paging(page = 2, maxResults = 10)
            val listParameters = InboxDocumentListParameters().apply {
                this.paging = paging
            }
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned and paging is applied to the query") {
                    result shouldBe listOf(document)
                    verify { typedQuery.setFirstResult(paging.getFirstResult()) }
                    verify { typedQuery.setMaxResults(paging.maxResults) }
                }
            }
        }

        Given("list parameters with ascending sorting") {
            val documents = listOf(createInboxDocument())
            val listParameters = InboxDocumentListParameters().apply {
                sorting = Sorting(InboxDocument.TITEL_PROPERTY_NAME, SorteerRichting.ASCENDING)
            }
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned and ascending order is applied") {
                    result shouldBe documents
                    verify { entityManager.criteriaBuilder.asc(any()) }
                }
            }
        }

        Given("list parameters with descending sorting") {
            val documents = listOf(createInboxDocument())
            val listParameters = InboxDocumentListParameters().apply {
                sorting = Sorting(InboxDocument.TITEL_PROPERTY_NAME, SorteerRichting.DESCENDING)
            }
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned and descending order is applied") {
                    result shouldBe documents
                    verify { entityManager.criteriaBuilder.desc(any()) }
                }
            }
        }

        Given("list parameters with titel and identificatie filters") {
            val documents = listOf(createInboxDocument())
            val listParameters = createInboxDocumentListParameters(
                title = "test document",
                identification = "DOC-001"
            )
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned") {
                    result shouldBe documents
                }
            }
        }

        Given("list parameters with a blank titel filter") {
            val documents = listOf(createInboxDocument())
            val listParameters = createInboxDocumentListParameters(title = "  ", identification = null)
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned without a titel predicate being applied") {
                    result shouldBe documents
                }
            }
        }

        Given("list parameters with a blank identificatie filter") {
            val documents = listOf(createInboxDocument())
            val listParameters = createInboxDocumentListParameters(title = null, identification = "  ")
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned without an identificatie predicate being applied") {
                    result shouldBe documents
                }
            }
        }

        Given("list parameters with a creatiedatum range") {
            val documents = listOf(createInboxDocument())
            val listParameters = createInboxDocumentListParameters(
                title = null,
                identification = null,
                creationDateRange = DatumRange(
                    van = LocalDate.of(2024, 1, 1),
                    tot = LocalDate.of(2024, 12, 31)
                )
            )
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned") {
                    result shouldBe documents
                }
            }
        }

        Given("list parameters with only the creatiedatum van boundary") {
            val documents = listOf(createInboxDocument())
            val listParameters = createInboxDocumentListParameters(
                title = null,
                identification = null,
                creationDateRange = DatumRange(van = LocalDate.of(2024, 6, 1), tot = null)
            )
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned") {
                    result shouldBe documents
                }
            }
        }

        Given("list parameters with only the creatiedatum tot boundary") {
            val documents = listOf(createInboxDocument())
            val listParameters = createInboxDocumentListParameters(
                title = null,
                identification = null,
                creationDateRange = DatumRange(van = null, tot = LocalDate.of(2024, 6, 30))
            )
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { resultList } returns documents
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("list is called") {
                val result = inboxDocumentRepository.list(listParameters)

                Then("results are returned") {
                    result shouldBe documents
                }
            }
        }
    }

    Context("Counting inbox documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 0L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(InboxDocumentListParameters())

                Then("zero is returned") {
                    result shouldBe 0
                }
            }
        }

        Given("inbox documents exist") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 5L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(InboxDocumentListParameters())

                Then("the document count is returned") {
                    result shouldBe 5
                }
            }
        }

        Given("the query returns null as the count result") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns null
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(InboxDocumentListParameters())

                Then("zero is returned as fallback") {
                    result shouldBe 0
                }
            }
        }

        Given("list parameters with titel and identificatie filters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 3L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(
                    createInboxDocumentListParameters(
                        title = "report",
                        identification = "DOC-42"
                    )
                )

                Then("the filtered count is returned") {
                    result shouldBe 3
                }
            }
        }

        Given("list parameters with a full creatiedatum range") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 2L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(
                    createInboxDocumentListParameters(
                        title = null,
                        identification = null,
                        creationDateRange = DatumRange(
                            van = LocalDate.of(2024, 1, 1),
                            tot = LocalDate.of(2024, 12, 31)
                        )
                    )
                )

                Then("the date-filtered count is returned") {
                    result shouldBe 2
                }
            }
        }

        Given("list parameters with only the creatiedatum van boundary") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 4L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(
                    createInboxDocumentListParameters(
                        title = null,
                        identification = null,
                        creationDateRange = DatumRange(van = LocalDate.of(2024, 6, 1), tot = null)
                    )
                )

                Then("the date-filtered count is returned") {
                    result shouldBe 4
                }
            }
        }

        Given("list parameters with only the creatiedatum tot boundary") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 7L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = inboxDocumentRepository.count(
                    createInboxDocumentListParameters(
                        title = null,
                        identification = null,
                        creationDateRange = DatumRange(van = null, tot = LocalDate.of(2024, 6, 30))
                    )
                )

                Then("the date-filtered count is returned") {
                    result shouldBe 7
                }
            }
        }
    }
})
