/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

/**
 * This test assumes two zaken, one task and one document have been created in previously run tests.
 * Note that the document in question is the form data PDF which was created during the handling of the 'productaanvraag'.
 * @see NotificationsTest
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED)
class IndexerenRESTServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("""Two zaken, a task and a document have been created""") {
        When("""the reindexing endpoint is called for type 'zaak'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/herindexeren/ZAAK",
                addAuthorizationHeader = false
            )
            Then(
                """the response is successful"""
            ) {
                response.isSuccessful shouldBe true
            }
        }
        When("""the reindexing endpoint is called for type 'task'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/herindexeren/TAAK",
                addAuthorizationHeader = false
            )
            Then(
                """the response is successful"""
            ) {
                response.isSuccessful shouldBe true
            }
        }
        When("""the reindexing endpoint is called for type 'document'""") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/indexeren/herindexeren/DOCUMENT"
            )
            Then(
                """the response is successful"""
            ) {
                response.isSuccessful shouldBe true
            }
        }
    }
})
