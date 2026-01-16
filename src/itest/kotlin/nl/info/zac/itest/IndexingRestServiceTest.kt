/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_NO_CONTENT

class IndexingRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("ZAC is up and running and a raadpleger is logged in") {
        // thia endpoint requires no explicit authorisation, however to pass the basic authorisation
        // a user with at least one ZAC role must be logged in
        authenticate(RAADPLEGER_DOMAIN_TEST_1)

        When("the commit pending changes to search index endpoint is called") {
            val response = itestHttpClient.performPostRequest(
                url = "$ZAC_API_URI/indexeren/commit-pending-changes-to-search-index",
                requestBody = "".toRequestBody()
            )
            Then(
                "the response is successful"
            ) {
                response.code shouldBe HTTP_NO_CONTENT
            }
        }
    }
})
