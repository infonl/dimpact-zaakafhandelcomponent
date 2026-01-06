/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_NO_CONTENT

class IndexingRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("""ZAC is up and running""") {
        When("""the commit pending changes to search index endpoint is called""") {
            val response = itestHttpClient.performPostRequest(
                url = "$ZAC_API_URI/indexeren/commit-pending-changes-to-search-index",
                requestBody = "".toRequestBody()
            )
            Then(
                """the response is successful"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT
            }
        }
    }
})
