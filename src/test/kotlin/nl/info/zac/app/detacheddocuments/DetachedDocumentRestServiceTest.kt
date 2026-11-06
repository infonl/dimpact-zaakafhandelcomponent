/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocument
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentResult
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.document.detacheddocument.DetachedDocumentBulkService
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentResult
import nl.info.zac.document.detacheddocument.repository.model.createDetachedDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.search.SearchService

class DetachedDocumentRestServiceTest : BehaviorSpec({
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val detachedDocumentBulkService = mockk<DetachedDocumentBulkService>()
    val restDetachedDocumentConverter = mockk<RestDetachedDocumentConverter>()
    val userConverter = mockk<RestUserConverter>()
    val policyService = mockk<PolicyService>()
    val searchService = mockk<SearchService>()
    val detachedDocumentRestService = DetachedDocumentRestService(
        detachedDocumentService,
        detachedDocumentBulkService,
        restDetachedDocumentConverter,
        userConverter,
        policyService,
        searchService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deleting a detached document") {
        given("a user with the right to delete detached documents") {
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every { detachedDocumentBulkService.delete(1L) } just runs

            `when`("the delete endpoint is called") {
                detachedDocumentRestService.deleteDetachedDocument(1L)

                then("the document is deleted") {
                    verify(exactly = 1) { detachedDocumentBulkService.delete(1L) }
                }
            }
        }

        given("a user without the right to delete detached documents") {
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = false)

            `when`("the delete endpoint is called") {
                val policyException = shouldThrow<PolicyException> {
                    detachedDocumentRestService.deleteDetachedDocument(2L)
                }

                then("the request is refused and nothing is deleted") {
                    policyException.message shouldBe null
                    verify(exactly = 0) { detachedDocumentBulkService.delete(2L) }
                }
            }
        }
    }

    context("Listing detached documents") {
        given("access to the inbox is denied") {
            val werklijstRechten = createWerklijstRechten(inbox = false)
            every {
                policyService.readWerklijstRechten()
            } returns werklijstRechten

            `when`("the list endpoint is called") {
                val exception = shouldThrow<PolicyException> {
                    detachedDocumentRestService.listDetachedDocuments(
                        RestDetachedDocumentListParameters()
                    )
                }

                then("a PolicyException is thrown") {
                    exception shouldNotBe null
                }
            }
        }

        given("a valid request with no ontkoppeldDoor filter in request or database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val restListParameters = RestDetachedDocumentListParameters()
            val detachedDocument = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(detachedDocument), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                detachedDocumentBulkService.findEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns informatieObject
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(detachedDocument.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(listOf(detachedDocument), any(), emptySet())
            } returns listOf(restDocument)

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                then("the result is returned with an empty filterOntkoppeldDoor") {
                    result shouldNotBe null
                    result.filterOntkoppeldDoor shouldBe emptyList()
                }
            }
        }

        given("a detached document that no longer exists in Open Zaak") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val detachedDocument = createDetachedDocument()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(detachedDocument), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                detachedDocumentBulkService.findEnkelvoudigInformatieobject(detachedDocument.documentUUID)
            } returns null
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(detachedDocument.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(
                    listOf(detachedDocument),
                    listOf(null),
                    emptySet()
                )
            } returns listOf(restDocument)

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(RestDetachedDocumentListParameters())

                then("the list still loads, with the row kept so that an employee can remove it") {
                    result.results shouldBe listOf(restDocument)
                    result.resultCount shouldBe 1L
                }
            }
        }

        given("a valid request with ontkoppeldDoor set in the request but empty in the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val requestUser = createRestUser(username = "fakeUsername1", name = "fakeUserName1")
            val restListParameters = RestDetachedDocumentListParameters().apply {
                ontkoppeldDoor = requestUser
            }
            val document = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val resultaat = DetachedDocumentResult(listOf(document), 1L, emptyList())
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                detachedDocumentBulkService.findEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(document.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(listOf(document), any(), emptySet())
            } returns listOf(restDocument)

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                then("filterOntkoppeldDoor contains the user from the request") {
                    result.filterOntkoppeldDoor shouldBe listOf(requestUser)
                }
            }
        }

        given("a valid request with ontkoppeldDoor filter returned from the database") {
            val werklijstRechten = createWerklijstRechten(inbox = true)
            val restListParameters = RestDetachedDocumentListParameters()
            val document = createDetachedDocument()
            val informatieObject = createEnkelvoudigInformatieObject()
            val restDocument = RestDetachedDocument()
            val dbUsernames = listOf("fakeUsername1", "fakeUsername2")
            val convertedUsers = listOf(
                createRestUser(username = "fakeUsername1", name = "fakeUserName1"),
                createRestUser(username = "fakeUsername2", name = "fakeUserName2")
            )
            val resultaat = DetachedDocumentResult(listOf(document), 1L, dbUsernames)
            every { policyService.readWerklijstRechten() } returns werklijstRechten
            every { detachedDocumentService.getDetachedDocumentResult(any()) } returns resultaat
            every {
                detachedDocumentBulkService.findEnkelvoudigInformatieobject(document.documentUUID)
            } returns informatieObject
            every {
                searchService.listZaakspecifiekGeautoriseerdeWaarden(listOf(document.zaakID))
            } returns emptySet()
            every {
                restDetachedDocumentConverter.convertToRestDetachedDocuments(listOf(document), any(), emptySet())
            } returns listOf(restDocument)
            every { with(userConverter) { dbUsernames.convertUsernames(any()) } } returns convertedUsers

            `when`("the list endpoint is called") {
                val result = detachedDocumentRestService.listDetachedDocuments(restListParameters)

                then("filterOntkoppeldDoor is populated from the database filter via userConverter") {
                    result.filterOntkoppeldDoor shouldBe convertedUsers
                }
            }
        }
    }
})
