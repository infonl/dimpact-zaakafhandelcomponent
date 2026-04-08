/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.zac.app.informatieobjecten.exception.DetachedDocumentNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.document.detacheddocument.model.DetachedDocument
import nl.info.zac.document.detacheddocument.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.model.createDetachedDocument
import java.time.LocalDate
import java.util.UUID

class DetachedDocumentServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentService = DetachedDocumentService(
        entityManager,
        loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating an detached document") {
        Given("a valid EnkelvoudigInformatieObject for a given Zaak") {
            val documentUuid = UUID.randomUUID()
            val identificatie = "DOC-456"
            val creatiedatum = LocalDate.now()
            val titel = "Ontkoppeld Document"
            val bestandsnaam = "ontkoppeld.pdf"
            val reden = "Test reden"
            val userId = "user-123"
            val zaak = createZaak(identificatie = "ZAAK-789")

            val informatieobject = createEnkelvoudigInformatieObject(
                uuid = documentUuid
            ).apply {
                setIdentificatie(identificatie)
                setCreatiedatum(creatiedatum)
                setTitel(titel)
                setBestandsnaam(bestandsnaam)
            }

            every { entityManager.persist(any<DetachedDocument>()) } just Runs
            every { loggedInUserInstance.get() } returns mockk<LoggedInUser> {
                every { id } returns userId
            }

            When("the ontkoppelde documenten create is invoked") {
                val result = detachedDocumentService.create(informatieobject, zaak, reden)

                Then("a detached document is created and stored") {
                    result.documentUUID shouldBe documentUuid
                    result.documentID shouldBe identificatie
                    result.creatiedatum shouldBe creatiedatum
                    result.titel shouldBe titel
                    result.bestandsnaam shouldBe bestandsnaam
                    result.ontkoppeldDoor shouldBe userId
                    result.zaakID shouldBe zaak.identificatie
                    result.reden shouldBe reden
                    result.ontkoppeldOp shouldNotBe null
                }
            }
        }
    }

    Context("Reading a detached document by UUID") {
        Given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val detachedDocument = createDetachedDocument(uuid = targetUuid)
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { resultList } returns listOf(detachedDocument)
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            When("read is called with that UUID") {
                val result = detachedDocumentService.read(targetUuid)

                Then("the document with that UUID is returned") {
                    result.documentUUID shouldBe targetUuid
                }
            }
        }

        Given("no detached document for a certain UUID") {
            val targetUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { getResultList() } returns emptyList()
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            When("read is called with that UUID") {
                val detachedDocumentNotFoundException = shouldThrow<DetachedDocumentNotFoundException> {
                    detachedDocumentService.read(targetUuid)
                }

                Then("an exception is thrown") {
                    detachedDocumentNotFoundException.message shouldBe
                        "No detached document found for enkelvoudiginformatieobject UUID: '$targetUuid'"
                }
            }
        }
    }

    Context("Finding a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createDetachedDocument()

            every { entityManager.find(DetachedDocument::class.java, document.id) } returns document

            When("find is called with that ID") {
                val result = detachedDocumentService.find(document.id!!)

                Then("the document is returned") {
                    result shouldBe document
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            every { entityManager.find(DetachedDocument::class.java, id) } returns null

            When("find is called with that ID") {
                val result = detachedDocumentService.find(id)

                Then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Getting a result set of detached documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { resultList } returns emptyList()
                every { singleResult } returns 0L
            }
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery

            When("getResultaat is called") {
                val result = detachedDocumentService.getDetachedDocumentResult(DetachedDocumentListParameters())

                Then("an empty result set is returned with count zero and no ontkoppeldDoor filter") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.detachedByFilter shouldBe emptyList()
                }
            }
        }
    }

    Context("Deleting a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createDetachedDocument()

            every { entityManager.find(DetachedDocument::class.java, document.id) } returns document

            When("delete is called with that ID") {
                detachedDocumentService.delete(document.id!!)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L

            every { entityManager.find(DetachedDocument::class.java, id) } returns null

            When("delete is called with that ID") {
                detachedDocumentService.delete(id)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }

    Context("Deleting a detached document by UUID") {
        Given("an existing detached document with a known UUID") {
            val targetUuid = UUID.randomUUID()
            val document = createDetachedDocument(uuid = targetUuid)
            val typedQuery = mockk<TypedQuery<DetachedDocument>> {
                every { resultList } returns listOf(document)
            }

            every {
                entityManager.createQuery(any<CriteriaQuery<DetachedDocument>>())
            } returns typedQuery

            When("delete is called with that UUID") {
                detachedDocumentService.delete(targetUuid)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }
    }
})
