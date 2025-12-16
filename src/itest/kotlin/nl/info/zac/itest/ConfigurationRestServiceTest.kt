/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.BRP_PROTOCOLLERING_ICONNECT
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_CODE
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_MAX_FILE_SIZE_IN_MB
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import java.net.HttpURLConnection.HTTP_OK

class ConfigurationRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("Configuration items are available in ZAC and a user with at least one ZAC role is logged in") {
        authenticate(RAADPLEGER_DOMAIN_TEST_1)

        When("the languages are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/talen"
            )

            Then("the available languages are returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
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
        When("the default language is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/talen/default"
            )

            Then("the default language is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
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
        When("the max upload file size is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/max-file-size-mb"
            )

            Then("the max upload file size is returned") {
                response.code shouldBe HTTP_OK
                response.bodyAsString.toLong() shouldBe CONFIG_MAX_FILE_SIZE_IN_MB
            }
        }
        When("the additional file types are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/additional-allowed-file-types"
            )

            Then("no additional file types are returned because ZAC does not provide any by default") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson """
                    [ "fakeFileExtension1", "fakeFileExtension2"]
                """.trimIndent()
            }
        }
        When("the council name is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/gemeente"
            )

            Then("the council name is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "\"$CONFIG_GEMEENTE_NAAM\""
            }
        }
        When("the council code is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/gemeente/code"
            )

            Then("the council code is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "\"$CONFIG_GEMEENTE_CODE\""
            }
        }
        When("the feature flag 'BPMN support' is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/feature-flags/bpmn-support"
            )

            Then("'true' is returned because BPMN support is enabled for the integration tests") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "true"
            }
        }
        When("the feature flag 'PABC integration' is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/feature-flags/pabc-integration"
            )

            Then("'true' is returned if the PABC integration flag is enabled, 'false' otherwise") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson if (FEATURE_FLAG_PABC_INTEGRATION) "true" else "false"
            }
        }
        When("the BRP protocollering provider is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/brp/protocollering-provider"
            )

            Then("the configured BRP protocollering provider is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldBe BRP_PROTOCOLLERING_ICONNECT
            }
        }
    }
})
