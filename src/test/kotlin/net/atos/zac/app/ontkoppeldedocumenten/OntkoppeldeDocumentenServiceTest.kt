/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten

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
import net.atos.zac.document.OntkoppeldeDocumentenService
import net.atos.zac.document.model.OntkoppeldDocument
import net.atos.zac.document.model.OntkoppeldDocumentListParameters
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.model.createOntkoppeldDocument
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class OntkoppeldeDocumentenServiceTest : BehaviorSpec({

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

            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { persist(any<OntkoppeldDocument>()) } just Runs
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>> {
                every { get() } returns mockk<LoggedInUser> {
                    every { id } returns userId
                }
            }

            val ontkoppeldeDocumentenService = OntkoppeldeDocumentenService(
                entityManager,
                loggedInUserInstance
            )

            When("the ontkoppelde documenten create is invoked") {
                val result = ontkoppeldeDocumentenService.create(informatieobject, zaak, reden)

                Then("an OntkoppeldDocument is created and stored") {
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
            val document = createOntkoppeldDocument(uuid = targetUuid)
            val entityManager = mockk<EntityManager>(relaxed = true)
            val typedQuery = mockk<TypedQuery<OntkoppeldDocument>> {
                every { getSingleResult() } returns document
            }
            every {
                entityManager.createQuery(any<CriteriaQuery<OntkoppeldDocument>>())
            } returns typedQuery
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("read is called with that UUID") {
                val result = service.read(targetUuid)

                Then("the document with that UUID is returned") {
                    result.documentUUID shouldBe targetUuid
                }
            }
        }
    }

    Context("Finding a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createOntkoppeldDocument()
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, document.id) } returns document
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("find is called with that ID") {
                val result = service.find(document.id)

                Then("an Optional containing the document is returned") {
                    result shouldBe Optional.of(document)
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, id) } returns null
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("find is called with that ID") {
                val result = service.find(id)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }

    Context("Getting a result set of detached documents") {
        Given("a relaxed entity manager and empty list parameters") {
            val entityManager = mockk<EntityManager>(relaxed = true)
            val typedQuery = mockk<TypedQuery<Long>>(relaxed = true) {
                every { getSingleResult() } returns null
            }
            // Due to JVM type erasure, any<CriteriaQuery<Long>>() matches all createQuery() calls
            // (for OntkoppeldDocument, Long, and String queries). The relaxed mock returns emptyList()
            // for getResultList() and null for getSingleResult(), exercising the null-count branch.
            every { entityManager.createQuery(any<CriteriaQuery<Long>>()) } returns typedQuery
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("getResultaat is called") {
                val result = service.getResultaat(OntkoppeldDocumentListParameters())

                Then("an empty result set is returned with count zero and no ontkoppeldDoor filter") {
                    result.items shouldBe emptyList()
                    result.count shouldBe 0L
                    result.ontkoppeldDoorFilter shouldBe emptyList()
                }
            }
        }
    }

    Context("Deleting a detached document by Long ID") {
        Given("an existing detached document with a known Long ID") {
            val document = createOntkoppeldDocument()
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, document.id) } returns document
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("delete is called with that ID") {
                service.delete(document.id)

                Then("the document is removed from the entity manager") {
                    verify { entityManager.remove(document) }
                }
            }
        }

        Given("no document exists for a given Long ID") {
            val id = 999L
            val entityManager = mockk<EntityManager>(relaxed = true) {
                every { find(OntkoppeldDocument::class.java, id) } returns null
            }
            val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
            val service = OntkoppeldeDocumentenService(entityManager, loggedInUserInstance)

            When("delete is called with that ID") {
                service.delete(id)

                Then("no document is removed from the entity manager") {
                    verify(exactly = 0) { entityManager.remove(any()) }
                }
            }
        }
    }
})
