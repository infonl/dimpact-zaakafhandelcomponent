/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import java.util.UUID

class DetachedDocumentRepositoryTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val detachedDocumentRepository = DetachedDocumentRepository(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Saving a detached document") {
        Given("a detached document entity") {
            val document = createDetachedDocument()

            When("save is called") {
                detachedDocumentRepository.save(document)

                Then("the entity is persisted") {
                    verify { entityManager.persist(document) }
                }
            }
        }
    }

    Context("Finding a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createDetachedDocument()

            every { entityManager.find(DetachedDocument::class.java, document.id) } returns document

            When("find is called with that ID") {
                val result = detachedDocumentRepository.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            every { entityManager.find(DetachedDocument::class.java, id) } returns null

            When("find is called with that ID") {
                val result = detachedDocumentRepository.find(id)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Finding a detached document by UUID") {
        Given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val document = createDetachedDocument(uuid = targetUuid)
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { resultList } returns listOf(document)
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            When("find is called with that UUID") {
                val result = detachedDocumentRepository.find(targetUuid)

                Then("the document with that UUID is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no detached document for a certain UUID") {
            val targetUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { resultList } returns emptyList()
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            When("find is called with that UUID") {
                val result = detachedDocumentRepository.find(targetUuid)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Deleting a detached document") {
        Given("an existing detached document") {
            val detachedDocument = createDetachedDocument()

            When("delete is called with that ID") {
                detachedDocumentRepository.delete(detachedDocument)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(detachedDocument) }
                }
            }
        }
    }

    Context("Listing detached documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<DetachedDocument>>(relaxed = true) {
                every { resultList } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>()) } returns typedQuery

            When("list is called") {
                val result = detachedDocumentRepository.list(DetachedDocumentListParameters())

                Then("an empty list is returned") {
                    result shouldBe emptyList()
                }
            }
        }
    }

    Context("Counting detached documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 0L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("count is called") {
                val result = detachedDocumentRepository.count(DetachedDocumentListParameters())

                Then("zero is returned") {
                    result shouldBe 0
                }
            }
        }
    }
})
