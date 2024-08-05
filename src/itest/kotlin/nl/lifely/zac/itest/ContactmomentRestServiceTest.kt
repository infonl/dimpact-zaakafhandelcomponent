/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

class ContactmomentRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("Klant contactmomenten are present in the OpenKlant database for a test customer") {
        When("the list contactmomenten endpoint is called with the BSN of this test customer") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/klanten/contactmomenten",
                requestBodyAsString = """
                    {
                        "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                        "page": 1
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the customer contactmomenten") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJson """
                    {
                      "foutmelding": "",
                      "resultaten": [
                        {
                          "initiatiefnemer": "First of Last",
                          "kanaal": "email",
                          "medewerker": "Actor Name",
                          "registratiedatum": "2000-01-01T12:00:00Z",
                          "tekst": "email contact"
                        },
                        {
                          "initiatiefnemer": "Name in Family",
                          "kanaal": "telefoon",
                          "registratiedatum": "2010-01-01T12:00:00Z",
                          "tekst": "phone contact"
                        }
                      ],
                      "totaal": 2
                    }
                """.trimIndent()
            }
        }
    }
})
