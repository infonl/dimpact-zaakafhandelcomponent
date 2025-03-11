/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * This test assumes two zaken, one task and one document have been created in previously run tests.
 * Note that the document in question is the form data PDF which was created during the handling of the 'productaanvraag'.
 * @see NotificationsTest
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED)
class IndexingRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("""ZAC is up and running""") {
        When("""the commit pending changes to search index endpoint is called""") {
            val response = itestHttpClient.performPostRequest(
                url = "$ZAC_API_URI/indexeren/commit-pending-changes-to-search-index",
                requestBody = "".toRequestBody(),
                addAuthorizationHeader = false
            )
            Then(
                """the response is successful"""
            ) {
                response.isSuccessful shouldBe true
            }
        }
    }
})
