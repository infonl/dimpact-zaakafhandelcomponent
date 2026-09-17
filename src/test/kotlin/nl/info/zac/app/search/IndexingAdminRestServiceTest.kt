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
import jakarta.ws.rs.core.Response
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType

class IndexingAdminRestServiceTest : BehaviorSpec({
    val indexingService = mockk<IndexingService>()
    val indexingAdminRestService = IndexingAdminRestService(indexingService = indexingService)

    afterEach {
        checkUnnecessaryStub()
    }

    given("a single object type to reindex that is not already being reindexed") {
        every { indexingService.reindexAsync(ZoekObjectType.ZAAK) } returns true

        `when`("the reindex endpoint is called") {
            val response = indexingAdminRestService.reindex(ZoekObjectType.ZAAK)

            then("the endpoint responds 202 Accepted, delegating the async launch to IndexingService") {
                response.status shouldBe Response.Status.ACCEPTED.statusCode
            }
        }
    }

    given("a single object type that is already being reindexed") {
        every { indexingService.reindexAsync(ZoekObjectType.ZAAK) } returns false

        `when`("the reindex endpoint is called") {
            val response = indexingAdminRestService.reindex(ZoekObjectType.ZAAK)

            then("the endpoint responds 409 Conflict instead of silently dropping the request") {
                response.status shouldBe Response.Status.CONFLICT.statusCode
            }
        }
    }

    given("all object types need to be reindexed") {
        every { indexingService.reindexAllAsync() } just runs

        `when`("the reindex-all endpoint is called") {
            val response = indexingAdminRestService.reindexAll()

            then("the endpoint responds 202 Accepted, delegating the async launch to IndexingService") {
                response.status shouldBe Response.Status.ACCEPTED.statusCode
            }
        }
    }
})
