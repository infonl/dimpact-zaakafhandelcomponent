/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainOnlyOnce
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers

const val INDEXING_COUNT = 10

/**
 * This test assumes two zaken, one task and one document have been created in previously run tests.
 * Note that the document in question is the form data PDF which was created during the handling of the 'productaanvraag'.
 * @see NotificationsTest
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_CREATED)
class IndexerenRESTServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("""Two zaken, a task and a document have been created""") {
        When("""the reindexing endpoint is called for type 'zaak'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/herindexeren/ZAAK",
                addAuthorizationHeader = false
            )
            Then(
                """the response indicates that the zaken have been marked to be reindexed to the search index"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("herindexeren", 0)
                    shouldContainJsonKeyValue("toevoegen", 2)
                    shouldContainJsonKeyValue("verwijderen", 0)
                }
            }
        }
        When("""the reindexing endpoint is called for type 'task'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/herindexeren/TAAK",
                addAuthorizationHeader = false
            )
            Then(
                """the response indicates that the taak has been marked to be added to the search index"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("herindexeren", 0)
                    shouldContainJsonKeyValue("toevoegen", 1)
                    shouldContainJsonKeyValue("verwijderen", 0)
                }
            }
        }
        When("""the reindexing endpoint is called for type 'document'""") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/indexeren/herindexeren/DOCUMENT"
            )
            Then(
                """the response indicates that the document has been marked to be added to the search index"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("herindexeren", 0)
                    shouldContainJsonKeyValue("toevoegen", 1)
                    shouldContainJsonKeyValue("verwijderen", 0)
                }
            }
        }
        When("""the perform indexing endpoint is called with amount '$INDEXING_COUNT'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/$INDEXING_COUNT",
                headers = Headers.headersOf("Accept", "text/plain")
            )
            Then(
                """the zaken, the task and the document should be indexed"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldContainOnlyOnce "[ZAAK] geindexeerd: 2, verwijderd: 0, resterend: 0"
                responseBody shouldContainOnlyOnce "[TAAK] geindexeerd: 1, verwijderd: 0, resterend: 0"
                responseBody shouldContainOnlyOnce "[DOCUMENT] geindexeerd: 1, verwijderd: 0, resterend: 0"
            }
        }
    }
})
