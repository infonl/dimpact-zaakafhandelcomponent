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

    afterEach {
        checkUnnecessaryStub()
    }

    context("Saving a detached document") {
        given("a detached document entity") {
            val document = createDetachedDocument()

            `when`("save is called") {
                detachedDocumentRepository.save(document)

                then("the entity is persisted") {
                    verify { entityManager.persist(document) }
                }
            }
        }
    }

    context("Finding a detached document by ID") {
        given("an existing detached document with a known ID") {
            val document = createDetachedDocument()

            every { entityManager.find(DetachedDocument::class.java, document.id) } returns document

            `when`("find is called with that ID") {
                val result = detachedDocumentRepository.find(document.id!!)

                then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        given("no document exists for a given ID") {
            val id = 999L
            every { entityManager.find(DetachedDocument::class.java, id) } returns null

            `when`("find is called with that ID") {
                val result = detachedDocumentRepository.find(id)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("Finding a detached document by UUID") {
        given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val document = createDetachedDocument(uuid = targetUuid)
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { resultList } returns listOf(document)
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            `when`("find is called with that UUID") {
                val result = detachedDocumentRepository.find(targetUuid)

                then("the document with that UUID is returned") {
                    result shouldBe document
                }
            }
        }

        given("no detached document for a certain UUID") {
            val targetUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { resultList } returns emptyList()
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            `when`("find is called with that UUID") {
                val result = detachedDocumentRepository.find(targetUuid)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("Deleting a detached document") {
        given("an existing detached document") {
            val detachedDocument = createDetachedDocument()

            `when`("delete is called for that document") {
                detachedDocumentRepository.delete(detachedDocument)

                then("the document is removed from the entity manager") {
                    verify { entityManager.remove(detachedDocument) }
                }
            }
        }
    }

    context("Listing detached documents") {
        given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<DetachedDocument>>(relaxed = true) {
                every { resultList } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>()) } returns typedQuery

            `when`("list is called") {
                val result = detachedDocumentRepository.list(DetachedDocumentListParameters())

                then("an empty list is returned") {
                    result shouldBe emptyList()
                }
            }
        }
    }

    context("Counting detached documents") {
        given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { singleResult } returns 0L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            `when`("count is called") {
                val result = detachedDocumentRepository.count(DetachedDocumentListParameters())

                then("zero is returned") {
                    result shouldBe 0
                }
            }
        }
    }
})
