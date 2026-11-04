/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.Validation
import nl.info.client.zgw.drc.DrcClientService
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkDeleteRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFailure
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFoutcode
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkLinkRequest
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.document.detacheddocument.DetachedDocumentBulkService
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailure
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkResult
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.search.SearchService

class DetachedDocumentRestServiceBulkTest : BehaviorSpec({
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val detachedDocumentBulkService = mockk<DetachedDocumentBulkService>()
    val drcClientService = mockk<DrcClientService>()
    val restDetachedDocumentConverter = mockk<RestDetachedDocumentConverter>()
    val userConverter = mockk<RestUserConverter>()
    val policyService = mockk<PolicyService>()
    val searchService = mockk<SearchService>()
    val detachedDocumentRestService = DetachedDocumentRestService(
        detachedDocumentService,
        detachedDocumentBulkService,
        drcClientService,
        restDetachedDocumentConverter,
        userConverter,
        policyService,
        searchService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Deleting detached documents in bulk") {
        given("a user with the right to delete detached documents") {
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = true)
            every { detachedDocumentBulkService.deleteInBulk(listOf(11L, 12L)) } returns DetachedDocumentBulkResult(
                processedIds = listOf(11L),
                failures = listOf(
                    DetachedDocumentBulkFailure(12L, "fakeTitel", DetachedDocumentBulkFailureReason.LOCKED)
                )
            )

            `when`("the bulk delete endpoint is called") {
                val result = detachedDocumentRestService.deleteDetachedDocuments(
                    RestDetachedDocumentBulkDeleteRequest(ids = listOf(11L, 12L))
                )

                then("the result of the bulk service is returned as the REST result") {
                    result.verwerkt shouldBe listOf(11L)
                    result.mislukt shouldBe listOf(
                        RestDetachedDocumentBulkFailure(12L, "fakeTitel", RestDetachedDocumentBulkFoutcode.VERGRENDELD)
                    )
                }
            }
        }

        given("a user without the right to delete detached documents") {
            every { policyService.readWerklijstRechten() } returns
                createWerklijstRechten(ontkoppeldeDocumentenVerwijderen = false)

            `when`("the bulk delete endpoint is called") {
                val policyException = shouldThrow<PolicyException> {
                    detachedDocumentRestService.deleteDetachedDocuments(
                        RestDetachedDocumentBulkDeleteRequest(ids = listOf(71L))
                    )
                }

                then("the request is refused before any document is touched") {
                    policyException.message shouldBe null
                    verify(exactly = 0) { detachedDocumentBulkService.deleteInBulk(any()) }
                }
            }
        }
    }

    context("Linking detached documents in bulk") {
        val nieuweZaakID = "fakeZaakId"

        given("a user with access to the inbox") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = true)
            every { detachedDocumentBulkService.linkInBulk(listOf(81L, 82L), nieuweZaakID) } returns
                DetachedDocumentBulkResult(
                    processedIds = listOf(81L),
                    failures = listOf(
                        DetachedDocumentBulkFailure(82L, "fakeTitel", DetachedDocumentBulkFailureReason.NO_RIGHTS)
                    )
                )

            `when`("the bulk link endpoint is called") {
                val result = detachedDocumentRestService.linkDetachedDocuments(
                    RestDetachedDocumentBulkLinkRequest(ids = listOf(81L, 82L), nieuweZaakID = nieuweZaakID)
                )

                then("the result of the bulk service is returned as the REST result") {
                    result.verwerkt shouldBe listOf(81L)
                    result.mislukt shouldBe listOf(
                        RestDetachedDocumentBulkFailure(82L, "fakeTitel", RestDetachedDocumentBulkFoutcode.GEEN_RECHT)
                    )
                }
            }
        }

        given("a user without access to the inbox") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten(inbox = false)

            `when`("the bulk link endpoint is called") {
                val policyException = shouldThrow<PolicyException> {
                    detachedDocumentRestService.linkDetachedDocuments(
                        RestDetachedDocumentBulkLinkRequest(ids = listOf(111L), nieuweZaakID = nieuweZaakID)
                    )
                }

                then("the request is refused before the zaak or any document is touched") {
                    policyException.message shouldBe null
                    verify(exactly = 0) { detachedDocumentBulkService.linkInBulk(any(), any()) }
                }
            }
        }
    }

    context("Validating a bulk request") {
        val validator = Validation.buildDefaultValidatorFactory().validator

        given("a bulk request with 101 ids") {
            val request = RestDetachedDocumentBulkDeleteRequest(ids = (1L..101L).toList())

            `when`("the request is validated") {
                val violations = validator.validate(request)

                then("it is refused") {
                    violations.size shouldBe 1
                }
            }
        }

        given("a bulk request with 100 ids") {
            val request = RestDetachedDocumentBulkDeleteRequest(ids = (1L..100L).toList())

            `when`("the request is validated") {
                val violations = validator.validate(request)

                then("it is accepted") {
                    violations.size shouldBe 0
                }
            }
        }

        given("a bulk link request without ids") {
            val request = RestDetachedDocumentBulkLinkRequest(ids = emptyList(), nieuweZaakID = "fakeZaakId")

            `when`("the request is validated") {
                val violations = validator.validate(request)

                then("it is refused") {
                    violations.size shouldBe 1
                }
            }
        }
    }
})
