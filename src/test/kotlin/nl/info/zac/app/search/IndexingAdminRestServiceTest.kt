/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class IndexingAdminRestServiceTest : BehaviorSpec({
    val indexingService = mockk<IndexingService>()
    val testDispatcher = StandardTestDispatcher()
    val indexingAdminRestService = IndexingAdminRestService(
        indexingService = indexingService,
        dispatcher = testDispatcher
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("a single object type to reindex") {
        every { indexingService.reindex(ZoekObjectType.ZAAK) } just runs

        `when`("the reindex endpoint is called") {
            lateinit var response: Response
            runTest(testDispatcher) {
                response = indexingAdminRestService.reindex(ZoekObjectType.ZAAK)
            }

            then("the endpoint responds 202 Accepted and the object type is reindexed asynchronously") {
                response.status shouldBe Response.Status.ACCEPTED.statusCode
                verify(exactly = 1) {
                    indexingService.reindex(ZoekObjectType.ZAAK)
                }
            }
        }
    }

    given("all object types need to be reindexed") {
        every { indexingService.reindexAll() } just runs

        `when`("the reindex-all endpoint is called") {
            lateinit var response: Response
            runTest(testDispatcher) {
                response = indexingAdminRestService.reindexAll()
            }

            then("the endpoint responds 202 Accepted and all object types are reindexed asynchronously") {
                response.status shouldBe Response.Status.ACCEPTED.statusCode
                verify(exactly = 1) {
                    indexingService.reindexAll()
                }
            }
        }
    }
})
