/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_DOCUMENTS
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_TASKS
import nl.info.zac.itest.config.ItestConfiguration.TOTAL_COUNT_ZAKEN
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

/**
 * This test assumes two zaken, one task and one document have been created in previously run tests.
 * Note that the document in question is the form data PDF which was created during the handling of the 'productaanvraag'.
 * @see NotificationsTest
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAKEN_TAKEN_DOCUMENTEN_ADDED)
class IndexingRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("""Two zaken, a task and a document have been created""") {
        When("""the reindexing endpoint is called for type 'zaak'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/herindexeren/ZAAK",
                addAuthorizationHeader = false
            )
            Then(
                """the response is successful and all zaken are indexed"""
            ) {
                response.isSuccessful shouldBe true
                // wait for the indexing to complete
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                           {
                            "filtersType": "ZoekParameters",
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": false,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": {},
                            "filters": {},
                            "datums": {},
                            "rows": 100,
                            "page": 0,
                            "type": "ZAAK"
                        }
                        """.trimIndent()
                    )
                    JSONObject(response.body!!.string()).getInt("totaal") shouldBe TOTAL_COUNT_ZAKEN
                }
            }
        }
        When("""the reindexing endpoint is called for type 'task'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/indexeren/herindexeren/TAAK",
                addAuthorizationHeader = false
            )
            Then(
                """the response is successful and all tasks are indexed"""
            ) {
                response.isSuccessful shouldBe true
                // wait for the indexing to complete
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                           {
                            "filtersType": "ZoekParameters",
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": false,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken":{},
                            "filters": {},
                            "datums": {},
                            "rows": 100,
                            "page": 0,
                            "type":"TAAK"
                        }
                        """.trimIndent()
                    )
                    JSONObject(response.body!!.string()).getInt("totaal") shouldBe TOTAL_COUNT_TASKS
                }
            }
        }
        When("""the reindexing endpoint is called for type 'document'""") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/indexeren/herindexeren/DOCUMENT"
            )
            Then(
                """the response is successful and all documents are indexed"""
            ) {
                response.isSuccessful shouldBe true
                // wait for the indexing to complete
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                           {
                            "filtersType": "ZoekParameters",
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": false,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": {},
                            "filters": {},
                            "datums": {},
                            "rows": 100,
                            "page": 0,
                            "type":"DOCUMENT"
                        }
                        """.trimIndent()
                    )
                    JSONObject(response.body!!.string()).getInt("totaal") shouldBe TOTAL_COUNT_DOCUMENTS
                }
            }
        }
    }
})
