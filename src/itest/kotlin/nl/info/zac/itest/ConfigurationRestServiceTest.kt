/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_CODE
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_MAX_FILE_SIZE_IN_MB
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder

class ConfigurationRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("Configuration items are available in ZAC") {
        When("the talen are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/talen"
            )

            Then("the available talen are returned") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrder """
                [ 
                    {
                      "code" : "dut",
                      "id" : "1",
                      "local" : "Nederlands",
                      "naam" : "Nederlands",
                      "name" : "Dutch"
                    }, 
                    {
                      "code" : "fre",
                      "id" : "2",
                      "local" : "français",
                      "naam" : "Frans",
                      "name" : "French"
                    },
                    {
                      "code" : "eng",
                      "id" : "3",
                      "local" : "English",
                      "naam" : "Engels",
                      "name" : "English"
                    },
                    {
                      "code" : "ger",
                      "id" : "4",
                      "local" : "Deutsch",
                      "naam" : "Duits",
                      "name" : "German"
                    },
                    {
                      "code" : "fry",
                      "id" : "5",
                      "local" : "Frysk",
                      "naam" : "Fries",
                      "name" : "Frisian"
                    }
                ]
                """.trimIndent()
            }
        }
        When("the default taal is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/talen/default"
            )

            Then("the default taal is returned") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson """
                {
                  "code" : "dut",
                  "id" : "1",
                  "local" : "Nederlands",
                  "naam" : "Nederlands",
                  "name" : "Dutch"
                }
                """.trimIndent()
            }
        }
        When("the max file size is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/max-file-size-mb"
            )

            Then("the max file size is returned") {
                response.code shouldBe HTTP_STATUS_OK
                response.body!!.string().toLong() shouldBe CONFIG_MAX_FILE_SIZE_IN_MB
            }
        }
        When("the additional file types are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/additional-allowed-file-types"
            )

            Then("no additional file types are returned because ZAC does not provide any by default") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson """
                    [ "dummyFileExtension1", "dummyFileExtension2"]
                """.trimIndent()
            }
        }
        When("the gemeente name is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/gemeente"
            )

            Then("the gemeente name is returned") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "\"$CONFIG_GEMEENTE_NAAM\""
            }
        }
        When("the gemeente code is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/gemeente/code"
            )

            Then("the gemeente code is returned") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "\"$CONFIG_GEMEENTE_CODE\""
            }
        }
        When("the feature flag 'BPMN support' is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/feature-flags/bpmn-support"
            )

            Then("'true' is returned because BPMN support is enabled for the integration tests") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "true"
            }
        }
    }
})
