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
    }
})
