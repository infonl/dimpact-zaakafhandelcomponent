/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.document

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import net.atos.zac.document.model.InboxDocument
import net.atos.zac.document.model.InboxDocumentListParameters
import net.atos.zac.document.model.createInboxDocument
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.zrc.ZrcClientService
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class InboxDocumentServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val inboxDocumentService = InboxDocumentService(entityManager, zrcClientService, drcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Creating an inbox document") {
        Given("an EnkelvoudigInformatieObject is available from the DRC service by UUID") {
            val uuid = UUID.randomUUID()
            val identificatie = "DOC-123"
            val creatiedatum = LocalDate.now()
            val titel = "fakeDocument"
            val bestandsnaam = "document.pdf"

            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
                setIdentificatie(identificatie)
                setCreatiedatum(creatiedatum)
                setTitel(titel)
                setBestandsnaam(bestandsnaam)
            }

            every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
            every { entityManager.persist(any<InboxDocument>()) } just Runs

            When(
                "the Inbox Document Service retrieves creates a Document from the EnkelvoudigInformatieObject's UUID"
            ) {
                val result = inboxDocumentService.create(uuid)

                Then("the Service should have stored an Inbox Document") {
                    verify { entityManager.persist(result) }
                    result.enkelvoudiginformatieobjectUUID shouldBe uuid
                    result.enkelvoudiginformatieobjectID shouldBe identificatie
                    result.creatiedatum shouldBe creatiedatum
                    result.titel shouldBe titel
                    result.bestandsnaam shouldBe bestandsnaam
                }
            }
        }
    }

    Context("Finding an inbox document by Long ID") {
        Given("an inbox document exists with a known Long ID") {
            val document = createInboxDocument()
            every { entityManager.find(InboxDocument::class.java, document.id) } returns document

            When("find is called with that ID") {
                val result = inboxDocumentService.find(document.id)

                Then("an Optional containing the document is returned") {
                    result shouldBe Optional.of(document)
                }
            }
        }

        Given("no inbox document exists for a given Long ID") {
            val id = 999L
            every { entityManager.find(InboxDocument::class.java, id) } returns null

            When("find is called with that ID") {
                val result = inboxDocumentService.find(id)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }

    Context("Finding an inbox document by UUID") {
        Given("an inbox document exists with a known UUID") {
            val document = createInboxDocument()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns listOf(document)
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(document.enkelvoudiginformatieobjectUUID)

                Then("an Optional containing the document is returned") {
                    result shouldBe Optional.of(document)
                }
            }
        }

        Given("no inbox document exists for a given UUID") {
            val unknownUuid = UUID.randomUUID()
            val typedQuery = mockk<TypedQuery<InboxDocument>>(relaxed = true) {
                every { getResultList() } returns emptyList()
            }
            every { entityManager.createQuery(any<CriteriaQuery<InboxDocument>>()) } returns typedQuery

            When("find is called with that UUID") {
                val result = inboxDocumentService.find(unknownUuid)

                Then("an empty Optional is returned") {
                    result shouldBe Optional.empty()
                }
            }
        }
    }
})
