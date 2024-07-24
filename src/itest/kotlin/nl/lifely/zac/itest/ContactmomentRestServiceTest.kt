/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject

class ContactmomentRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("Klant contactmomenten are present in the OpenKlant database for a test customer") {
        When("the list contactmomenten endpoint is called with the BSN of this test customer") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/contactmomenten",
                requestBodyAsString = """
                    {
                    "bsn":"$TEST_PERSON_HENDRIKA_JANSE_BSN",
                    "page":0,
                    "pageSize":10
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the customer contactmomenten") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("totaal", 2.0)
                    shouldContainJsonKey("resultaten")
                    val resultaten = JSONObject(responseBody).getJSONArray("resultaten")
                    resultaten.length() shouldBe 2
                    with(JSONArray(resultaten)[0].toString()) {
                        shouldContainJsonKeyValue("initiatiefnemer", "klant")
                        shouldContainJsonKeyValue("kanaal", "dummyContactMomentCommunicationChannel2")
                        shouldContainJsonKeyValue("tekst", "dummyContactMomentText2")
                        shouldContainJsonKeyValue("registratiedatum", "2010-01-01T12:00:00Z")
                    }
                    with(JSONArray(resultaten)[1].toString()) {
                        shouldContainJsonKeyValue("initiatiefnemer", "klant")
                        shouldContainJsonKeyValue("kanaal", "dummyContactMomentCommunicationChannel1")
                        shouldContainJsonKeyValue("tekst", "dummyContactMomentText1")
                        shouldContainJsonKeyValue("registratiedatum", "2000-01-01T12:00:00Z")
                    }
                }
            }
        }
    }
})
